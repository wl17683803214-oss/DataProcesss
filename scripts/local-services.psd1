@{
    # 本机现有的开发工具及依赖安装目录，移动软件后在这里修改。
    JavaHome = 'C:\Program Files\Java\jdk1.8.0_181'
    # 已安装的数据库与缓存使用现有系统服务，不重复启动独立进程。
    KingbaseServiceName = 'kingbase8_R6_instance'
    RedisServiceName = 'Redis'
    # 数据库和缓存沿用当前项目的本机端口。
    KingbasePort = 54321
    RedisPort = 6379
    # 配置中心沿用原有单机配置、配置数据和账号设置。
    NacosHome = 'D:\nacos'
    NacosPort = 8848
    NacosGrpcPort = 9848
    # 从原安装目录启动，继续使用其中的greptimedb_data数据目录。
    GreptimeHome = 'D:\greptime-windows-amd64-v1.2.1'
    GreptimeHttpPort = 4000
    GreptimeGrpcPort = 4001
    GreptimeMysqlPort = 4002
    # 显式指定已有本机代理配置，名称服务和代理必须使用同一个地址。
    RocketmqHome = 'D:\rocketmq-all-5.5.0-bin-release'
    BrokerConfig = 'D:\rocketmq-all-5.5.0-bin-release\conf\broker-local.conf'
    NameServerPort = 9876
    BrokerPort = 10911
    # 对象存储继续使用现有程序、数据目录、账号和端口。
    MinioExecutable = 'D:\Minio\minio.windows-amd64.RELEASE.2025-09-07T16-13-09Z.exe'
    MinioDataDirectory = 'D:\Minio\data'
    MinioRootUser = 'minioadmin'
    MinioRootPassword = 'minioadmin123'
    MinioApiPort = 9002
    MinioConsolePort = 9003
    # 每个依赖的最大启动等待时间，单位秒。
    StartupTimeoutSeconds = 120
    # 相对目录以项目根目录为基准；每次启动新进程生成独立日志。
    LogDirectory = 'data/local-services/logs'
}
