package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.local.*;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

/**
 * 1. use local storage
 * 2. create a vm
 * 3. create a data volume
 * 4. attach the data volume to the vm
 *
 * confirm the data volume is allocated on the same host with vm's root volume
 */
public class TestLocalStorage9 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException {
        DiskOfferingInventory dof = deployer.diskOfferings.get("TestDiskOffering1");
        VolumeInventory data = api.createDataVolume("data", dof.getUuid());
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.attachVolumeToVm(vm.getUuid(), data.getUuid());

        Assert.assertFalse(config.createEmptyVolumeCmds.isEmpty());

        LocalStorageResourceRefVO rootRef = dbf.findByUuid(vm.getRootVolumeUuid(), LocalStorageResourceRefVO.class);
        Assert.assertNotNull(rootRef);
        LocalStorageResourceRefVO dataRef = dbf.findByUuid(data.getUuid(), LocalStorageResourceRefVO.class);
        Assert.assertNotNull(dataRef);
        Assert.assertEquals(rootRef.getHostUuid(), dataRef.getHostUuid());
        Assert.assertEquals(rootRef.getPrimaryStorageUuid(), dataRef.getPrimaryStorageUuid());
        Assert.assertEquals(data.getSize(), dataRef.getSize());
    }
}
