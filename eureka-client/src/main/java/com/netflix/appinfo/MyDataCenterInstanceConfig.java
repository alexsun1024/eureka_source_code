/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.netflix.appinfo;

import com.google.inject.ProvidedBy;
import com.netflix.appinfo.providers.MyDataCenterInstanceConfigProvider;

import javax.inject.Singleton;

/**
 * An {@link InstanceInfo} configuration for the non-AWS datacenter.
 *它没有任何自己的方法实现，就是确定了namespace的值使用全局的，也就是默认是eureka。
 * @author Karthik Ranganathan
 *
 */
@Singleton   //@javax.inject.Inject，一个新的注入依赖规范，既能支持Google Gucie，还能支持Spring
@ProvidedBy(MyDataCenterInstanceConfigProvider.class) //Guice是一个轻量级的依赖注入框架。用于对象之间的依赖的注入。
public class MyDataCenterInstanceConfig extends PropertiesInstanceConfig implements EurekaInstanceConfig {

    public MyDataCenterInstanceConfig() {
    }

    public MyDataCenterInstanceConfig(String namespace) {
        super(namespace);
    }

    public MyDataCenterInstanceConfig(String namespace, DataCenterInfo dataCenterInfo) {
        super(namespace, dataCenterInfo);
    }

}
