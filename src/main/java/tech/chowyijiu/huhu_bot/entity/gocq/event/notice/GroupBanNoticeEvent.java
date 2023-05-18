package tech.chowyijiu.huhu_bot.entity.gocq.event.notice;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import tech.chowyijiu.huhu_bot.constant.NoticeTypeEnum;

/**
 * @author elastic chow
 * @date 18/5/2023
 */
@Getter
@Setter
@ToString
public class GroupBanNoticeEvent extends NoticeEvent {
    private final String noticeType = NoticeTypeEnum.group_ban.name();

    private String subType; //approve、invite 事件子类型, 分别表示管理员已同意入群、管理员邀请入群
    private Long userId;
    private Long groupId;
    private Long operatorId;
    private Long duration;
}
