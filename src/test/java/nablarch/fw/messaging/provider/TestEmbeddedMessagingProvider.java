package nablarch.fw.messaging.provider;

import nablarch.core.repository.initialization.Initializable;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;

import javax.jms.Queue;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * JVM内蔵式メッセージングサーバによる簡易メッセージングプロバイダ実装。
 * 
 * この実装では、サブスレッド上で動作するJMSプロバイダ実装を内蔵しており、
 * そこに接続して動作する。
 * これにより、外部のMOMを用意すること無くメッセージング処理を含んだ業務機能の
 * 単体テストを実施することが可能である。
 * 
 * 現時点の実装では、自動テストでの利用のみを想定しているため、
 * リモートキューへの転送はサポートしていない。
 * また、内部的にActiveMQのメッセージブローカーとvm:// プロトコルを使用しているため、
 * 本機能を利用する場合は、ActiveMQのライブラリをクラスパスに含める必要がある。
 * 
 * @author Iwauo Tajima
 */
public class TestEmbeddedMessagingProvider extends TestJmsMessagingProvider implements Initializable {
    // ------------------------------------------------------- structure
    /** 内蔵サーバ */
    private static Server server;
    
    
    // --------------------------------------------------------- Constructors
    /**
     * コンストラクタ
     */
    public TestEmbeddedMessagingProvider() {
        if (server == null) {
            server = new Server().start();
        }
        setConnectionFactory(new ActiveMQConnectionFactory("vm://localhost"));
    }
    
    // --------------------------------------------- Managing server instance
    /**
     * 内蔵サーバを停止する。
     */
    public static void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }
    
    /**
     * 内蔵サーバが起動するまでカレントスレッドを待機させる。
     * 
     * @throws InterruptedException
     *          割り込み要求が発生した場合。もしくは、5分以上経過しても
     *          起動が完了しなかった場合。
     */
    public static void waitUntilServerStarted() throws InterruptedException {
        if (server != null) {
            server.waitUntilStarted();
        }
    }
    
    // ---------------------------------------------------------- Accessors
    /**
     * このキューマネージャが管理するキューの論理名を設定する。
     * (既存の設定は全て削除される。)
     * @param names キュー名の一覧
     * @return このオブジェクト自体
     */
    public TestEmbeddedMessagingProvider setQueueNames(List<String> names) {
        Map<String, Queue> table = new HashMap<String, Queue>();
        for (String queueName : names) {
            table.put(queueName, new ActiveMQQueue(queueName));
        }
        server.setDestinations(table);
        setDestinations(table);
        return this;
    }

    /**
     * 内蔵サーバ
     */
    private static class Server {
        /** キューマネージャ */
        private BrokerService broker;
        
        /** 起動待機用ラッチ */
        private static CountDownLatch startupLatch = new CountDownLatch(1);
        
        /**
         * キューの論理名と実装オブジェクトの対応を設定する。
         * @param queueTable キューの論理名と実装オブジェクトの対応
         * @return このオブジェクト自体
         */
        public Server setDestinations(Map<String, Queue> queueTable) {
            broker.setDestinations(
                Collections.list(Collections.enumeration(queueTable.values()))
                           .toArray(new ActiveMQQueue[]{})
            );
            return this;
        }
        
        /**
         * コンストラクタ
         */
        public Server() {
            try {
                broker = new BrokerService();
                broker.setPersistent(false);
                broker.setUseJmx(false);
                broker.addConnector("vm://localhost");
            } catch (Exception e) {
                throw new RuntimeException(
                    "an Error occurred while launch the messaging broker", e
                );
            }
        }

        /**
         * 内蔵サーバを起動する。
         * @return このオブジェクト自体
         */
        public Server start() {
            try {
                broker.start();
                broker.waitUntilStarted();
                startupLatch.countDown();
                
            } catch (Exception e) {
                stop();
                throw new RuntimeException(e);
            }
            return this;
        }
        
        /**
         * 内蔵サーバを停止させる。
         * @return このオブジェクト自体
         */
        public Server stop() {
            try {
                broker.stop();
                broker.waitUntilStopped();
                broker = null;
                startupLatch = new CountDownLatch(1);
                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return this;
        }
        
        /**
         * サーバが起動するまでwaitする。
         * @return このオブジェクト自体。
         * @throws InterruptedException
         *          割り込み要求が発生した場合。もしくは、5分以上経過しても
         *          起動が完了しなかった場合。
         */
        public Server waitUntilStarted() throws InterruptedException {
            startupLatch.await(300, TimeUnit.SECONDS);
            return this;
        }
    }

    /**
     * {@inheritDoc}<br/>
     * 他の実装クラスとインタフェースを合わせるために{@link Initializable}を実装する。
     * {@link Initializable}を実装することで、リクエスト単体テスト時に
     * {@link nablarch.core.repository.initialization.ApplicationInitializer}の
     * リポジトリ設定の上書きを不要にしている。
     * 本メソッドは何も処理しない。
     */
    public void initialize() {
    }
}
