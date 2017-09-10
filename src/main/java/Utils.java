
import com.thoughtworks.xstream.XStream;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {

  public static String getSha1(String str){
    if (null == str || 0 == str.length()){
      return null;
    }
    char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      'a', 'b', 'c', 'd', 'e', 'f'};
    try {
      MessageDigest mdTemp = MessageDigest.getInstance("SHA1");
      mdTemp.update(str.getBytes("UTF-8"));

      byte[] md = mdTemp.digest();
      int j = md.length;
      char[] buf = new char[j * 2];
      int k = 0;
      for (int i = 0; i < j; i++) {
        byte byte0 = md[i];
        buf[k++] = hexDigits[byte0 >>> 4 & 0xf];
        buf[k++] = hexDigits[byte0 & 0xf];
      }
      return new String(buf);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    return null;
  }
  //把对象转化为xml
  public static String TextToXML(DataBean dataBean){
    try {
      XStream xStream = new XStream();
      //xStream.alias("xml",dataBean.getClass());
      System.out.println(xStream.toXML(dataBean).toString());
      return xStream.toXML(dataBean);//

    }catch (Exception e){
      e.printStackTrace();
      return  "";

    }
  }


}
