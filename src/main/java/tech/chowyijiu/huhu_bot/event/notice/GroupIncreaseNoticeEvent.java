package tech.chowyijiu.huhu_bot.event.notice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tech.chowyijiu.huhu_bot.constant.NoticeTypeEnum;

/**
 * 群成员增加事件
 * @author elastic chow
 * @date 18/5/2023
 */
@Getter
@Setter
@ToString
public class GroupIncreaseNoticeEvent extends NoticeEvent {
    private final String noticeType = NoticeTypeEnum.group_increase.name();

    private String subType; //approve、invite 事件子类型, 分别表示管理员已同意入群、管理员邀请入群
    private Long userId;
    private Long groupId;
    private Long operatorId;
}