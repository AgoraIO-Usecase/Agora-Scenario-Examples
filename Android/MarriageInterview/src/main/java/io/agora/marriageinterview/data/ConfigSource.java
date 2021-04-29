package io.agora.marriageinterview.data;

import com.agora.data.provider.DefaultConfigSource;

/**
 * 配置文件
 *
 * @author chenhengfei(Aslanchen)
 */
public class ConfigSource extends DefaultConfigSource {

    @Override
    public String getMemberTableName() {
        return "MEMBER_MARRY";
    }

    @Override
    public String getActionTableName() {
        return "ACTION_MARRY";
    }

    @Override
    public String getRoomTableName() {
        return "ROOM_MARRY";
    }
}
