package tech.chowyijiu.huhu_bot.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.CollectionUtils;
import tech.chowyijiu.huhu_bot.constant.GocqActionEnum;
import tech.chowyijiu.huhu_bot.entity.gocq.request.RequestBox;
import tech.chowyijiu.huhu_bot.entity.gocq.response.*;
import tech.chowyijiu.huhu_bot.ws.Bot;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author elastic chow
 * @date 14/5/2023
 */
@Slf4j
public class GocqSyncRequestUtil {
    private GocqSyncRequestUtil() {
    }

    public static int poolSize = Runtime.getRuntime().availableProcessors() + 1;
    public static long sleep = 5000L;
    public static final ExecutorService pool =
            new ThreadPoolExecutor(poolSize, poolSize * 2, 24L, TimeUnit.HOURS,
            new SynchronousQueue<>(), new CustomizableThreadFactory("pool-sendSyncMessage-"));

    private static final Map<String, JSONObject> resultMap = new ConcurrentHashMap<>();

    public static void putEchoResult(String key, JSONObject val) {
        resultMap.put(key, val);
    }

    /**
     * 获取消息详情对象
     * @param messageId
     * @param timeout
     * @return
     */
    public static WsResp getMsg(Bot bot, String messageId, long timeout) {
        Map<String, Object> map = new HashMap<>(1);
        map.put("message_id", messageId);
        JSONObject jsonObject = sendSyncRequest(bot, GocqActionEnum.GET_MSG, map, timeout);
        if (jsonObject != null) {
            return JSONObject.parseObject(jsonObject.getString("data"), WsResp.class);
        }
        return null;
    }


    public static SelfInfo getLoginInfo(Bot bot, long timeout) {
        JSONObject responseStr = sendSyncRequest(bot, GocqActionEnum.GET_LOGIN_INGO, null, timeout);
        if (responseStr != null) {
            SelfInfo data = JSONObject.parseObject(responseStr.getString("data"), SelfInfo.class);
            return data;
        }
        return null;
    }

    /**
     * 获取群成员
     *
     * @param groupId 群号
     * @param exclude 需要排除的成员qq号
     * @return
     */
    public static List<GroupMember> getGroupMemberList(Bot bot, Long groupId, List<Long> exclude, long timeout) {
        Map<String, Object> params = new HashMap<>(1);
        params.put("group_id", groupId);
        JSONObject jsonObject = sendSyncRequest(bot, GocqActionEnum.GET_GROUP_MEMBER_LIST, params, timeout);
        if (jsonObject == null) {
            return null;
        }
        String dataStr = jsonObject.getString("data");
        if (Strings.isBlank(dataStr)) {
            return null;
        }
        List<GroupMember> data = JSONArray.parseArray(dataStr, GroupMember.class);
        if (!CollectionUtils.isEmpty(exclude) && !CollectionUtils.isEmpty(data)) {
            data.removeIf(next -> exclude.contains(next.getUserId()));
        }
        return data;
    }

    /**
     * 发送私聊文件
     * @param userId
     * @param filePath 该文件必须与gocqhttp在同一主机上
     * @param fileName
     * @param timeout
     * @return
     */
    public static SyncResponse uploadPrivateFile(Bot bot, Long userId, String filePath, String fileName, long timeout) {
        Map<String, Object> param = new HashMap<>(3);
        param.put("user_id", userId);
        param.put("file", filePath);
        param.put("name", fileName);
        JSONObject responseStr = sendSyncRequest(bot, GocqActionEnum.UPLOAD_PRIVATE_FILE, param, timeout);
        if (responseStr != null) {
            SyncResponse response = JSONObject.parseObject(responseStr.toJSONString(), SyncResponse.class);
            return response;
        }
        return null;
    }

    /**
     * 用gocq去下载文件
     * @param url
     * @param threadCount
     * @param httpHeaders
     * @param timeout
     * @return 返回gocq下载到的文件绝对路径
     */
    public static DownloadFileResp downloadFile(Bot bot, String url, int threadCount, HttpHeaders httpHeaders, long timeout) {

        Map<String, Object> param = new HashMap<>(3);
        param.put("url", url);
        param.put("thread_count", threadCount);

        if (httpHeaders != null && !httpHeaders.isEmpty()) {
            List<String> headStrs = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
                StringBuilder item = new StringBuilder(entry.getKey() + "=");
                for (String s : entry.getValue()) {
                    item.append(s).append(";");
                }
                headStrs.add(item.toString());
            }
            param.put("headers", JSONObject.toJSONString(headStrs));

        }
        JSONObject jsonObject = sendSyncRequest(bot, GocqActionEnum.DOWNLOAD_FILE, param, timeout);
        if (jsonObject != null) {
            return jsonObject.getObject("data", DownloadFileResp.class);
        }
        return null;
    }


    /***
     * 发送同步消息
     * @param action 终结点
     * @param params 参数
     * @param timeout 超时 ms
     * @param <T>
     * @return
     */
    public static <T> JSONObject sendSyncRequest(Bot bot, GocqActionEnum action, T params, long timeout) {
        RequestBox<T> requestBox = new RequestBox<>();
        if (params != null) {
            requestBox.setParams(params);
        }
        requestBox.setAction(action.getAction());
        String echo = Thread.currentThread().getName() + "_" +
                bot.getUserId() + "_" +
                action.getAction() + "_" +
                UUID.randomUUID().toString().replace("-","");
        requestBox.setEcho(echo);
        //bot.sendMessage(JSONObject.toJSONString(requestBox));
        log.info("echo: {}", echo);
        FutureTask<JSONObject> futureTask = new FutureTask<>(new GocqSyncRequestUtil.Task(echo));
        pool.submit(futureTask);
        try {
            JSONObject res;
            if (timeout <= sleep) {
                res = futureTask.get();
            } else {
                res = futureTask.get(timeout, TimeUnit.MILLISECONDS);
            }
            log.info("echo: {},result: {}", echo, res);
            return res;
        } catch (InterruptedException e) {
            log.error("发送同步消息线程中断异常,echo:{}", echo, e);
        } catch (ExecutionException e) {
            log.error("发送同步消息执行异常,echo:{}", echo, e);
        } catch (TimeoutException e) {
            log.error("发送同步消息超时,echo:{}", echo, e);
        } catch (Exception e) {
            log.error("发送同步消息异常,echo:{}", echo, e);
        } finally {
            futureTask.cancel(true);
            resultMap.remove(echo);
        }
        return null;
    }

    private static class Task implements Callable<JSONObject> {
        private String echo;

        Task(String echo) {
            if (Strings.isBlank(echo)) {
                throw new IllegalArgumentException("echo is blank");
            }
            this.echo = echo;
        }

        @Override
        public JSONObject call() throws Exception {
            JSONObject res = null;
            while (!Thread.currentThread().isInterrupted()) {
                res = resultMap.get(echo);
                if (res != null) {
                    break;
                } else {
                    Thread.sleep(GocqSyncRequestUtil.sleep);
                }
            }
            return res;
        }
    }
}