import com.thoughtworks.xstream.XStream;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.dom4j.Document;

public class Main  extends AbstractVerticle{

  private static String to;
  private static String from;
  private static String type;
  private static String content;
  private static MongoClient client;
  private static String event;
  private static String access_token ="";


  //main 方法如果不是静态的就会报错
  public static void main(String[] args) {
    //部署
/*    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(Main.class.getName());*/
  }

  //请求用户数据
  public   void getUserInfo(String openID){
    WebClient client = WebClient.create(vertx);
    String  url = "https://api.weixin.qq.com/cgi-bin/user/info?access_token="+access_token+"&openid="+openID+"&lang=zh_CN ";
    client.get(url).send(res->{
      if (res.succeeded()){
        System.out.println("获取用户数据："+res.result());
      }else {
        System.out.println("失败："+res.cause().getMessage());
      }
    });
  }

  private   void   generateXml(String[] cards, boolean isSuccess, RoutingContext h) {
    String out;
    try {
      DataBean dataBean = new DataBean();
      dataBean.setCreateTime(System.currentTimeMillis()+"");
      dataBean.setFromUserName(to);
      dataBean.setMsgType("text");
      dataBean.setToUserName(from);
      if (isSuccess){
        dataBean.setContent(cards[0]+"数据添加成功");
      }else {
        dataBean.setContent("数据添加失败,请按照正确格式输入：\n银行名称#信用卡账单日#信用卡还款日。例如：\n中信银行#12#1");
      }
      //生成xml数据
      XStream xStream = new XStream();
      xStream.alias("xml",dataBean.getClass());
      out = xStream.toXML(dataBean);

    }catch (Exception e){
      e.printStackTrace();
      out ="";
    }
    h.response().end(out);

  }

  //代替main 方法作为启动器
  @Override
  public void start(Future<Void> startFuture) throws Exception {
    //vertx = Vertx.vertx();
    HttpServer httpServer = vertx.createHttpServer();
    client = MongoClient.createShared(vertx, new JsonObject().put("db_name", "creditcard"));
    Router router = Router.router(vertx);
    CookieHandler cookieHandler = CookieHandler.create();//cookies
    SessionHandler sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));//session
//    router.route().handler(cookieHandler).handler(sessionHandler);
    router.route().handler(BodyHandler.create());//

    //认证请求
    router.get().handler(h->{
      String out = processWxSignature(h);
      h.response().end(out);//返回数据

    });



    /**
     *  匹配所有post 请求
     */
    router.post().handler(h->{
      //1.收到client 请求后判断当前是否有token值
      if (access_token.equals("")){
        //异步请求获取token
        WebClient client = WebClient.create(vertx);
        String  url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=wxa60a2cdd4ea836ce&secret=c4891bf98394009896bf0f5fa092f1da";
        client.get(url).send(res->{
          if (res.succeeded()){
            //请求成功后处理之前的请求数据
            JsonObject object = res.result().bodyAsJsonObject();
            access_token = object.getString("access_token");
            System.out.println("ok="+access_token);
            //因为路由器问题，手动获取该token
            // access_token ="A-W_QsBi02z605NhqecnRwYIz-kp50C2iBwreLlqbT8QholWS-KRxxbGeqZCx7Qgu4OBzR8VnKrDgYmuEqa397eYX5p4YV7u4wgs6SBG5156Awg7K8mT_h6BVDhmbLXzZQLhAFARID";
            //获取token成功以后解析数据，并根据用户openid 获取用户信息
            String body = h.getBodyAsString();
            if (body!=null){
              System.out.println(body.toString());
              try {
                //2.解析数据
                parseXml(body,h);//解析xml
              } catch (Exception e) {
                e.printStackTrace();
              }

            }
            System.out.println("access_token="+ access_token);
          }else {
            System.out.println("access_token="+ res.cause().getMessage());
          }
        });
      }

    });


    httpServer.requestHandler(router::accept).listen(6688);



  }

  //插入信用卡数据
  private  void insertCreaditCard(RoutingContext h) {
    //输入的银行卡文本格式： 中信银行#12#1  （银行+账单日+还款日）

    String[] split = content.split("#");
    if (split==null ||split.length<3){
      generateXml(split,false,h);
      return;
    }

    JsonObject object = new JsonObject();
    object.put("name","lwz");//绑定人
    object.put("bank",split[0]);
    object.put("Statement_Date",split[1]);
    object.put("Repayment_Date",split[2]);

    //这里可能会回掉，异步方法
    client.insert("cardsInfo",object,res->{
        if (res.succeeded()){
          //插入成功 回复数据
          generateXml(split,true,h);
        } else {
          generateXml(split,false,h);
          System.out.println(res.cause());
        }
    });

  }


  //解析xml 引入jar包 dom4j
  private  void parseXml(String s, RoutingContext h) {
    HashMap<String,String> map = new HashMap<>();
    SAXReader reader = new SAXReader();//创建对象
    try {
      Document read = reader.read(new ByteArrayInputStream(s.getBytes("utf-8")));//读取数据,必须是数据流，
      Element root = read.getRootElement();//获取根
      List<Element> elements = root.elements();//获取所有节点
      for (Element item:elements){
        map.put(item.getName(),item.getStringValue());
      }
      to = map.get("ToUserName");
      from = map.get("FromUserName");
      type = map.get("MsgType");
      content = map.get("Content");
      event = map.get("Event");
    } catch (Exception e) {
      System.out.println("解析xml异常");
      e.printStackTrace();
    }
    /**
     * 根据消息类型来做出不同得响应
     */
    if ("event".equals(type)){
      //用户首次订阅
      if ("subscribe".equals(event)){


      }else {
        //取消订阅
      }

    }
    //获取用户信息
    getUserInfo(from);


    //1.查询数据
    System.out.println("content:"+content);
    if ("查询".equals(content)){
      DataBean dataBean = new DataBean();
      dataBean.setCreateTime(System.currentTimeMillis()+"");
      dataBean.setFromUserName(to);
      dataBean.setMsgType("text");
      dataBean.setToUserName(from);
      //查询所有数据
      StringBuilder  builder = new StringBuilder();
      client.find("cardsInfo",new JsonObject(),res->{
        if (res.succeeded()){
          System.out.println(res.result());
          int a=0;
          for (JsonObject item :res.result()){
            a++;
            builder.append(a+":"+item.getString("bank")+"-账单日 "+item.getString("Statement_Date")+
                    "-还款日 "+item.getString("Repayment_Date")+"\n");
          }
          System.out.println("查询所有"+builder.toString());
          dataBean.setContent(builder.toString());
        }else {
          dataBean.setContent("查询失败");
          System.out.println("查询失败");
        }
        //以xml 格式返回数据
        XStream xStream = new XStream();
        xStream.alias("xml",dataBean.getClass());
        h.response().end(xStream.toXML(dataBean));

      });
    }else {
      //解析完毕后插入数据库
      //2.插入数据
      insertCreaditCard(h);
    }



  }


    /**
     * 处理微信的绑定请求
     *
     1）将token、timestamp、nonce三个参数进行字典序排序
     2）将三个参数字符串拼接成一个字符串进行sha1加密
     3）开发者获得加密后的字符串可与signature对比，标识该请求来源于微信
     * @param h
     */
  private  String processWxSignature(RoutingContext h) {
    HttpServerRequest request = h.request();
    String signature = request.getParam("signature");
    String timestamp = request.getParam("timestamp");
    String nonce = request.getParam("nonce");
    String echostr = request.getParam("echostr");

    //参数排序
    ArrayList<String> list=new ArrayList<String>();
    list.add(nonce);
    list.add(timestamp);
    list.add("lwz");
    Collections.sort(list);
    //SHA1 加密
    String sha1 = Utils.getSha1(list.get(0) + list.get(1) + list.get(2));

    if (sha1!=null){
      if (sha1.equals(signature)){
        //签名认证成功
        System.out.println(echostr);
        return  echostr;
      }
    }
    return "fail";
  }
}
