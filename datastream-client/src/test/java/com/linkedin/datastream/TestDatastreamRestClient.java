package com.linkedin.datastream;

import java.io.IOException;
import java.util.Properties;

import com.linkedin.datastream.common.DatastreamSource;
import com.linkedin.datastream.connectors.DummyBootstrapConnector;
import com.linkedin.datastream.connectors.DummyBootstrapConnectorFactory;
import com.linkedin.datastream.connectors.DummyConnector;
import com.linkedin.datastream.connectors.DummyConnectorFactory;
import com.linkedin.datastream.server.DummyDatastreamEventCollector;
import com.linkedin.datastream.testutil.EmbeddedZookeeper;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.Assert;

import com.linkedin.data.template.StringMap;
import com.linkedin.datastream.common.Datastream;
import com.linkedin.datastream.common.DatastreamException;
import com.linkedin.datastream.common.DatastreamNotFoundException;
import com.linkedin.datastream.server.DatastreamServer;
import com.linkedin.r2.RemoteInvocationException;


@Test(singleThreaded=true)
public class TestDatastreamRestClient {
  // "com.linkedin.datastream.server.DummyDatastreamEventCollector"
  private static final String COLLECTOR_CLASS = DummyDatastreamEventCollector.class.getTypeName();
  Logger LOG = LoggerFactory.getLogger(TestDatastreamRestClient.class);

  private static final String DUMMY_CONNECTOR = DummyConnector.CONNECTOR_TYPE;
  private static final String DUMMY_BOOTSTRAP_CONNECTOR = DummyBootstrapConnector.CONNECTOR_TYPE;

  @BeforeTest
  public void setUp() throws Exception {
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
    DatastreamServer.INSTANCE.shutDown();
    setupServer();
  }

  @AfterTest
  public void tearDown() throws Exception {
    DatastreamServer.INSTANCE.shutDown();
  }

  public static Datastream generateDatastream(int seed) {
    Datastream ds = new Datastream();
    ds.setName("name_" + seed);
    ds.setConnectorType(DummyConnector.CONNECTOR_TYPE);
    ds.setSource(new DatastreamSource());
    ds.getSource().setConnectionString("DummySource");
    StringMap metadata = new StringMap();
    metadata.put("owner", "person_" + seed);
    ds.setMetadata(metadata);
    return ds;
  }

  private void setupServer()
      throws Exception {
    EmbeddedZookeeper embeddedZookeeper = new EmbeddedZookeeper();
    String zkConnectionString = embeddedZookeeper.getConnection();
    embeddedZookeeper.startup();

    Properties properties = new Properties();
    properties.put(DatastreamServer.CONFIG_CLUSTER_NAME, "testCluster");
    properties.put(DatastreamServer.CONFIG_ZK_ADDRESS, zkConnectionString);
    properties.put(DatastreamServer.CONFIG_EVENT_COLLECTOR_CLASS_NAME, COLLECTOR_CLASS);
    properties.put(DatastreamServer.CONFIG_HTTP_PORT, "8080");
    properties.put(DatastreamServer.CONFIG_CONNECTOR_TYPES, DUMMY_CONNECTOR + "," + DUMMY_BOOTSTRAP_CONNECTOR);
    properties.put(DUMMY_CONNECTOR + "." + DatastreamServer.CONFIG_CONNECTOR_FACTORY_CLASS_NAME,
                   DummyConnectorFactory.class.getTypeName());
    properties.put(DUMMY_BOOTSTRAP_CONNECTOR + "." + DatastreamServer.CONFIG_CONNECTOR_FACTORY_CLASS_NAME,
                   DummyBootstrapConnectorFactory.class.getTypeName());
    properties.put(DUMMY_CONNECTOR + "." + DatastreamServer.CONFIG_CONNECTOR_BOOTSTRAP_TYPE,
                   DUMMY_BOOTSTRAP_CONNECTOR);
    properties.put(DUMMY_CONNECTOR + ".dummyProperty", "dummyValue"); // DummyConnector will verify this value being correctly set
    DatastreamServer.INSTANCE.init(properties);
  }

  @Test
  public void testCreateDatastream()
      throws DatastreamException, IOException, RemoteInvocationException {
    Datastream datastream = generateDatastream(1);
    LOG.info("Datastream : " + datastream);
    DatastreamRestClient restClient = new DatastreamRestClient("http://localhost:8080/");
    restClient.createDatastream(datastream);
    Datastream createdDatastream = restClient.getDatastream(datastream.getName());
    LOG.info("Created Datastream : " + createdDatastream);
    Assert.assertEquals(createdDatastream, datastream);
  }

  @Test
  public void testGetBootstrapDatastream()
      throws IOException, DatastreamException, RemoteInvocationException {
    Datastream datastream = generateDatastream(2);
    LOG.info("Datastream : " + datastream);
    DatastreamRestClient restClient = new DatastreamRestClient("http://localhost:8080/");
    restClient.createDatastream(datastream);
    Datastream createdDatastream = restClient.getDatastream(datastream.getName());
    LOG.info("Created Datastream : " + createdDatastream);
    Datastream bootstrapDatastream = restClient.createBootstrapDatastream(datastream.getName());
    Assert.assertEquals(bootstrapDatastream.getConnectorType(), DUMMY_BOOTSTRAP_CONNECTOR);
    Assert.assertTrue(bootstrapDatastream.getName().startsWith(datastream.getName()));
  }

  @Test(expectedExceptions = DatastreamNotFoundException.class)
  public void testGetBootstrapDatastream_throwsDatastreamNotFoundException_whenDatastreamIsNotfound()
      throws IOException, DatastreamException, RemoteInvocationException {
    DatastreamRestClient restClient = new DatastreamRestClient("http://localhost:8080/");
    restClient.createBootstrapDatastream("Datastream_doesntexist");
  }

  @Test(expectedExceptions = DatastreamNotFoundException.class)
  public void testGetDatastream_throwsDatastreamNotFoundException_whenDatastreamIsNotfound()
      throws IOException, DatastreamException, RemoteInvocationException {
    DatastreamRestClient restClient = new DatastreamRestClient("http://localhost:8080/");
    restClient.getDatastream("Datastream_doesntexist");
  }

  @Test(expectedExceptions = DatastreamException.class)
  public void testCreateDatastream_throwsDatastreamException_onBadDatastream()
      throws IOException, DatastreamException, RemoteInvocationException {
    DatastreamRestClient restClient = new DatastreamRestClient("http://localhost:8080/");
    restClient.createDatastream(new Datastream());
  }
}