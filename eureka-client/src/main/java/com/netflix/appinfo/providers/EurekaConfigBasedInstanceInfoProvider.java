package com.netflix.appinfo.providers;

import javax.inject.Singleton;
import javax.inject.Provider;
import java.util.Map;

import com.google.inject.Inject;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.appinfo.InstanceInfo.PortType;
import com.netflix.appinfo.LeaseInfo;
import com.netflix.appinfo.RefreshableInstanceConfig;
import com.netflix.appinfo.UniqueIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InstanceInfo provider that constructs the InstanceInfo this this instance using
 * EurekaInstanceConfig.
 *
 * This provider is @Singleton scope as it provides the InstanceInfo for both DiscoveryClient
 * and ApplicationInfoManager, and need to provide the same InstanceInfo to both.
 *  使用EurekaInstanceConfig在此实例上构造InstanceInfo的InstanceInfo提供程序。 此提供程序是@Singleton范围，
 *  因为它同时为DiscoveryClient和ApplicationInfoManager提供InstanceInfo，并且需要为它们提供相同的InstanceInfo。
 * @author elandau
 *
 */
@Singleton
public class EurekaConfigBasedInstanceInfoProvider implements Provider<InstanceInfo> {
    private static final Logger LOG = LoggerFactory.getLogger(EurekaConfigBasedInstanceInfoProvider.class);

    private final EurekaInstanceConfig config;

    private InstanceInfo instanceInfo;

    @Inject(optional = true)
    private VipAddressResolver vipAddressResolver = null;

    @Inject
    public EurekaConfigBasedInstanceInfoProvider(EurekaInstanceConfig config) {
        this.config = config;
    }

    @Override
    public synchronized InstanceInfo get() {
        if (instanceInfo == null) {
            // Build the lease information to be passed to the server based on config   根据配置构建要传递给服务器的续约信息
            LeaseInfo.Builder leaseInfoBuilder = LeaseInfo.Builder.newBuilder()//续约（心跳）
                    .setRenewalIntervalInSecs(config.getLeaseRenewalIntervalInSeconds())//续约参数（心跳参数）
                    .setDurationInSecs(config.getLeaseExpirationDurationInSeconds());

            if (vipAddressResolver == null) {//虚拟局域网
                vipAddressResolver = new Archaius1VipAddressResolver();
            }

            // Builder the instance information to be registered with eureka server     生成要在eureka服务器上注册的实例信息
            InstanceInfo.Builder builder = InstanceInfo.Builder.newBuilder(vipAddressResolver); //建造者模式

            // set the appropriate id for the InstanceInfo, falling back to datacenter Id if applicable, else hostname为InstanceInfo设置适当的ID，如果适用，则回退到数据中心ID，否则返回主机名
            String instanceId = config.getInstanceId();
            if (instanceId == null || instanceId.isEmpty()) {
                DataCenterInfo dataCenterInfo = config.getDataCenterInfo();
                if (dataCenterInfo instanceof UniqueIdentifier) {
                    instanceId = ((UniqueIdentifier) dataCenterInfo).getId();
                } else {
                    instanceId = config.getHostName(false);
                }
            }

            String defaultAddress;
            if (config instanceof RefreshableInstanceConfig) {
                // Refresh AWS data center info, and return up to date address 刷新AWS数据中心信息，并返回最新地址
                defaultAddress = ((RefreshableInstanceConfig) config).resolveDefaultAddress(false);
            } else {
                defaultAddress = config.getHostName(false);
            }

            // fail safe
            if (defaultAddress == null || defaultAddress.isEmpty()) {
                defaultAddress = config.getIpAddress();
            }

            builder.setNamespace(config.getNamespace())
                    .setInstanceId(instanceId)
                    .setAppName(config.getAppname())
                    .setAppGroupName(config.getAppGroupName())
                    .setDataCenterInfo(config.getDataCenterInfo())
                    .setIPAddr(config.getIpAddress())
                    .setHostName(defaultAddress)
                    .setPort(config.getNonSecurePort())
                    .enablePort(PortType.UNSECURE, config.isNonSecurePortEnabled())
                    .setSecurePort(config.getSecurePort())
                    .enablePort(PortType.SECURE, config.getSecurePortEnabled())
                    .setVIPAddress(config.getVirtualHostName())
                    .setSecureVIPAddress(config.getSecureVirtualHostName())
                    .setHomePageUrl(config.getHomePageUrlPath(), config.getHomePageUrl())
                    .setStatusPageUrl(config.getStatusPageUrlPath(), config.getStatusPageUrl())
                    .setASGName(config.getASGName())
                    .setHealthCheckUrls(config.getHealthCheckUrlPath(),
                            config.getHealthCheckUrl(), config.getSecureHealthCheckUrl());


            // Start off with the STARTING state to avoid traffic  从STARTING状态开始以避免通信
            if (!config.isInstanceEnabledOnit()) {//指示在实例向eureka注册后是否应启用该实例以进行流量。 有时，应用程序可能需要做一些预处理才能准备进行流量处理。
                InstanceStatus initialStatus = InstanceStatus.STARTING;
                LOG.info("Setting initial instance status as: {}", initialStatus);
                builder.setStatus(initialStatus);
            } else {
                LOG.info("Setting initial instance status as: {}. This may be too early for the instance to advertise "
                         + "itself as available. You would instead want to control this via a healthcheck handler.",
                         InstanceStatus.UP);
            }

            // Add any user-specific metadata information  添加任何特定于用户的元数据信息
            for (Map.Entry<String, String> mapEntry : config.getMetadataMap().entrySet()) {
                String key = mapEntry.getKey();
                String value = mapEntry.getValue();
                // only add the metadata if the value is present
                if (value != null && !value.isEmpty()) {
                    builder.add(key, value);
                }
            }

            instanceInfo = builder.build();//实例相关
            instanceInfo.setLeaseInfo(leaseInfoBuilder.build());  //续约相关
        }
        return instanceInfo;
    }

}
