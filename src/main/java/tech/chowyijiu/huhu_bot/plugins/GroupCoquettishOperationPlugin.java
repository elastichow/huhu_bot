package tech.chowyijiu.huhu_bot.plugins;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import tech.chowyijiu.huhu_bot.annotation.BotPlugin;
import tech.chowyijiu.huhu_bot.annotation.MessageHandler;
import tech.chowyijiu.huhu_bot.annotation.NoticeHandler;
import tech.chowyijiu.huhu_bot.constant.GocqActionEnum;
import tech.chowyijiu.huhu_bot.constant.SubTypeEnum;
import tech.chowyijiu.huhu_bot.entity.gocq.message.MessageSegment;
import tech.chowyijiu.huhu_bot.entity.gocq.response.GroupInfo;
import tech.chowyijiu.huhu_bot.entity.gocq.response.GroupMember;
import tech.chowyijiu.huhu_bot.event.message.GroupMessageEvent;
import tech.chowyijiu.huhu_bot.event.notice.GroupIncreaseNoticeEvent;
import tech.chowyijiu.huhu_bot.event.notice.NotifyNoticeEvent;
import tech.chowyijiu.huhu_bot.ws.Bot;
import tech.chowyijiu.huhu_bot.ws.Server;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author elastic chow
 * @date 18/5/2023
 */
@Slf4j
@BotPlugin("群组骚操作")
public class GroupCoquettishOperationPlugin {

    @Scheduled(cron = "0 * * * * *  ")
    public void dateGroupCard() {
        String card = buildDateCard();
        Server.getBots().forEach(bot -> {
            Optional.ofNullable(bot.getGroups()).orElseGet(bot::getGroupList)
                    .stream().map(GroupInfo::getGroupId).forEach(groupId -> {
                        bot.callApi(GocqActionEnum.SET_GROUP_CARD,
                                "group_id", groupId, "user_id", bot.getUserId(), "card", card);
                        try {
                            Thread.sleep(2000L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
        });
    }

    private String buildDateCard() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime shitTime = LocalDateTime.parse("2023-05-27 17:00", formatter);
        Duration duration = Duration.between(shitTime, LocalDateTime.now());
        long days = duration.toDays();
        duration = duration.minusDays(days);
        long hours = duration.toHours();
        duration = duration.minusHours(hours);
        long minutes = duration.toMinutes();
        return "失业第" + days + "天" + hours + "时" + minutes + "分";
    }

    @MessageHandler(name = "头衔自助", commands = {"sgst"})
    public void sgst(Bot bot, GroupMessageEvent event) {
        //先判断bot是不是群主
        GroupMember groupMember = bot.getGroupMember(event.getGroupId(), bot.getUserId(), true);
        if (!"owner".equals(groupMember.getRole())) {
            log.info("{} 机器人不是群主, 忽略", "头衔自助");
            return;
        }
        if (event.getMessage().length() > 6) {
            bot.sendGroupMessage(event.getGroupId(), "群头衔最多为6位", true);
            return;
        }
        bot.callApi(GocqActionEnum.SET_GROUP_SPECIAL_TITLE,
                "group_id", event.getGroupId(), "user_id", event.getUserId(),
                "special_title", event.getMessage());
    }


    @NoticeHandler(name = "群内回戳", priority = 0)
    public void replyPoke(Bot bot, NotifyNoticeEvent event) {
        if (SubTypeEnum.poke.name().equals(event.getSubType())
            && bot.getUserId().equals(event.getTargetId())
            && !bot.getUserId().equals(event.getUserId())) {
            Optional.ofNullable(event.getGroupId())
                    .ifPresent(groupId -> bot.sendGroupMessage(
                            groupId, MessageSegment.poke(event.getUserId()) + "", false)
                    );
        }
    }


    @NoticeHandler(name = "清代肝")
    public void cleanDaiGan(Bot bot, GroupIncreaseNoticeEvent event) {
        GroupMember groupMember = bot.getGroupMember(event.getGroupId(), event.getUserId(), true);
        Optional.ofNullable(groupMember.getNickname()).ifPresent(nickname -> {
            if (nickname.contains("代肝")) {
                bot.kickGroupMember(event.getGroupId(), event.getUserId(), true);
            }
        });
        Optional.ofNullable(groupMember.getCard()).ifPresent(card -> {
            if (card.contains("代肝")) {
                bot.kickGroupMember(event.getGroupId(), event.getUserId(), true);
            }
        });
    }
}
