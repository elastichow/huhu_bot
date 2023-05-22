package tech.chowyijiu.huhu_bot.entity.gocq.message;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import tech.chowyijiu.huhu_bot.constant.CqTypeEnum;

import java.util.*;

/**
 * @author elastic chow
 * @date 19/5/2023
 */
@Data
@NoArgsConstructor
public class Message {

    private List<MessageSegment> messageSegments = new ArrayList<>();

    private String strMessage;

    /**
     * 字符串转Message对象, 用于转换事件的消息
     * @param str message
     * @return Message
     */
    public static Message toMessage(String str) {
        Message message = new Message();
        message.strMessage = str;
        if (!str.contains("[CQ:")) {
            message.append(str);
            return message;
        }
        int fromIndex = 0, start = 0, end = 0;
        //获取第一个索引
        while (true) {
            start = str.indexOf("[CQ:", fromIndex);
            //如果没有cq了, 直接把剩余的部分添加成text
            if (start == -1 ) {
                if (str.length() > fromIndex) {
                    message.append(str.substring(fromIndex));
                }
                break;
            }
            //如果cq的start前面还有字符串, 先把这一部分添加成text
            if (start - fromIndex > 0) {
                message.append(str.substring(fromIndex, start));
            }
            end = str.indexOf("]", start);
            //添加cq
            String cq = str.substring(start + 4, end);
            String[] split = cq.split(",");
            MessageSegment segment = new MessageSegment(CqTypeEnum.valueOf(split[0]).name());
            for (int i = 1; i < split.length; i++) {
                String[] keyVal = split[i].split("=");
                segment.addParam(keyVal[0], keyVal[1]);
            }
            message.append(segment);
            fromIndex = end + 1;
        }
        return message;
    }


    public boolean isToMe(Long selfId) {
        for (MessageSegment segment : messageSegments) {
            if (segment.getType().equals(CqTypeEnum.at.name())) {
                if (Objects.equals(Long.parseLong(segment.getData().get(0).value), selfId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 拼接str message
     */
    private void spliceStr() {
        StringBuilder sb = new StringBuilder();
        for (MessageSegment segment : this.messageSegments) {
            String type = segment.getType();
            if (Objects.equals(type, "text")) {
                sb.append(segment.getText());
            } else {
                sb.append("[CQ:").append(segment.getType());
                segment.getData().forEach(node -> sb.append(",").append(node.key).append("=").append(node.value));
                sb.append("]");
            }
        }
        this.strMessage = sb.toString();
    }


    /**
     * @param text String
     */
    public Message append(String text) {
        messageSegments.add(MessageSegment.text(text));
        return this;
    }

    /**
     * @param segment  MessageSegment
     */
    public Message append(MessageSegment segment) {
        messageSegments.add(segment);
        return this;
    }


    @Override
    public String toString() {
        if (Strings.isBlank(this.strMessage)) {
            spliceStr();
        }
        return this.strMessage;
    }

    /**
     * 用于测试, 变成json数组
     * @return
     */
    public String toJsonArray() {
        return Arrays.toString(messageSegments.toArray());
    }

}