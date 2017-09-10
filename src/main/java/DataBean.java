public class DataBean {

    /**
     *   <xml>
     *       <ToUserName><![CDATA[gh_3b7b037e868f]]></ToUserName>
     <FromUserName><![CDATA[oYK6-vlnpe9MLgdluE3PXfTHpAew]]></FromUserName>
     <CreateTime>1504237409</CreateTime>
     <MsgType><![CDATA[text]]></MsgType>
     <Content><![CDATA[咯哦哦]]></Content>
     <MsgId>6460650477537522929</MsgId>
     </xml>
     */

    public String ToUserName;
    public String FromUserName;
    public String CreateTime;
    public String MsgType;
    public String Content;
    public String MsgId;
    public String Event; //事件类型

    public String getEvent() {
        return Event;
    }

    public void setEvent(String event) {
        Event = event;
    }

    public String getToUserName() {
        return ToUserName;
    }

    public void setToUserName(String toUserName) {
        ToUserName = toUserName;
    }

    public String getFromUserName() {
        return FromUserName;
    }

    public void setFromUserName(String fromUserName) {
        FromUserName = fromUserName;
    }

    public String getCreateTime() {
        return CreateTime;
    }

    public void setCreateTime(String createTime) {
        CreateTime = createTime;
    }

    public String getMsgType() {
        return MsgType;
    }

    public void setMsgType(String msgType) {
        MsgType = msgType;
    }

    public String getContent() {
        return Content;
    }

    public void setContent(String content) {
        Content = content;
    }

    public String getMsgId() {
        return MsgId;
    }

    public void setMsgId(String msgId) {
        MsgId = msgId;
    }
}
