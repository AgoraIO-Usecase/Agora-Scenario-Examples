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
        return "MERRYMEMBER";
    }

    @Override
    public String getActionTableName() {
        return "MERRYACTION";
    }
}
