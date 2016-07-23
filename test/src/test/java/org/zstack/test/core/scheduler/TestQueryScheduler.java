package org.zstack.test.core.scheduler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.scheduler.*;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by root on 7/20/16.
 */
public class TestQueryScheduler {
    ComponentLoader loader;
    Api api;
    @Autowired
    SchedulerFacade scheduler;
    DatabaseFacade dbf;
    CloudBus bus;
    Deployer deployer;
    SessionInventory session;
    VolumeSnapshotKvmSimulator snapshotKvmSimulator;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        //BeanConstructor con = new BeanConstructor();
        //con.addXml("SchedulerFacade.xml");
        //loader = con.build();
        //dbf = loader.getComponent(DatabaseFacade.class);
        //api = new Api();
        //api.startServer();
        //session = api.loginAsAdmin();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("SchedulerFacade.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        bus = loader.getComponent(CloudBus.class);
        scheduler = loader.getComponent(SchedulerFacade.class);
        snapshotKvmSimulator = loader.getComponent(VolumeSnapshotKvmSimulator.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException, SchedulerException {
        SchedulerVO vo = new SchedulerVO();
        vo.setSchedulerName("test-query");
        vo.setSchedulerType("cron");
        vo.setCronScheduler("1 * * * * ?");
        vo.setStartDate(new Timestamp(1234567));
        vo.setUuid(Platform.getUuid());
        vo = dbf.persistAndRefresh(vo);


       // Assert.assertNotNull(scheduler);
       // VmInstanceInventory vm = deployer.vms.get("TestVm");
       // String volUuid = vm.getRootVolumeUuid();
       // api.createScheduler(volUuid, session);
       // TimeUnit.SECONDS.sleep(30);
       // long record = dbf.count(VolumeSnapshotVO.class);
       // Assert.assertEquals(3,record);
       // SchedulerVO vo = new SchedulerVO();


        SchedulerInventory inv = SchedulerInventory.valueOf(vo);
        APIQuerySchedulerMsg msg = new APIQuerySchedulerMsg();
        QueryTestValidator.validateEQ(msg, api, APIQuerySchedulerReply.class, inv);
        msg.setConditions(new ArrayList<QueryCondition>());
        APIQuerySchedulerReply reply = api.query(msg, APIQuerySchedulerReply.class);
        Assert.assertEquals(1, reply.getInventories().size());

    }

}
