package net.sf.briar.protocol;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import net.sf.briar.TestUtils;
import net.sf.briar.api.protocol.AuthorId;
import net.sf.briar.api.protocol.Batch;
import net.sf.briar.api.protocol.BatchBuilder;
import net.sf.briar.api.protocol.BatchId;
import net.sf.briar.api.protocol.BundleReader;
import net.sf.briar.api.protocol.BundleWriter;
import net.sf.briar.api.protocol.GroupId;
import net.sf.briar.api.protocol.Header;
import net.sf.briar.api.protocol.HeaderBuilder;
import net.sf.briar.api.protocol.Message;
import net.sf.briar.api.protocol.MessageId;
import net.sf.briar.api.protocol.MessageParser;
import net.sf.briar.api.protocol.UniqueId;
import net.sf.briar.api.serial.FormatException;
import net.sf.briar.api.serial.Reader;
import net.sf.briar.api.serial.Writer;
import net.sf.briar.api.serial.WriterFactory;
import net.sf.briar.serial.ReaderFactoryImpl;
import net.sf.briar.serial.WriterFactoryImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Provider;

public class BundleReadWriteTest extends TestCase {

	private static final String SIGNATURE_ALGO = "SHA256withRSA";
	private static final String KEY_PAIR_ALGO = "RSA";
	private static final String DIGEST_ALGO = "SHA-256";

	private final File testDir = TestUtils.getTestDirectory();
	private final File bundle = new File(testDir, "bundle");

	private final long capacity = 1024L;
	private final BatchId ack = new BatchId(TestUtils.getRandomId());
	private final Set<BatchId> acks = Collections.singleton(ack);
	private final GroupId sub = new GroupId(TestUtils.getRandomId());
	private final Set<GroupId> subs = Collections.singleton(sub);
	private final Map<String, String> transports =
		Collections.singletonMap("foo", "bar");

	private final MessageId messageId = new MessageId(TestUtils.getRandomId());
	private final AuthorId authorId = new AuthorId(TestUtils.getRandomId());
	private final long timestamp = System.currentTimeMillis();
	private final byte[] messageBody = new byte[123];
	private final Message message = new MessageImpl(messageId, MessageId.NONE,
			sub, authorId, timestamp, messageBody);

	// FIXME: This test should not depend on an impl in another component
	private final WriterFactory wf = new WriterFactoryImpl();

	private final KeyPair keyPair;
	private final Signature sig;
	private final MessageDigest digest;

	public BundleReadWriteTest() throws NoSuchAlgorithmException {
		super();
		keyPair = KeyPairGenerator.getInstance(KEY_PAIR_ALGO).generateKeyPair();
		sig = Signature.getInstance(SIGNATURE_ALGO);
		digest = MessageDigest.getInstance(DIGEST_ALGO);
		assertEquals(digest.getDigestLength(), UniqueId.LENGTH);
	}

	@Before
	public void setUp() {
		testDir.mkdirs();
	}

	@Test
	public void testWriteBundle() throws Exception {
		HeaderBuilder h = new OutgoingHeaderBuilder(keyPair, sig, digest, wf);
		h.addAcks(acks);
		h.addSubscriptions(subs);
		h.addTransports(transports);
		Header header = h.build();

		BatchBuilder b = new OutgoingBatchBuilder(keyPair, sig, digest, wf);
		b.addMessage(message);
		Batch batch = b.build();

		FileOutputStream out = new FileOutputStream(bundle);
		Writer writer = new WriterFactoryImpl().createWriter(out);
		BundleWriter w = new BundleWriterImpl(writer, capacity);

		w.addHeader(header);
		w.addBatch(batch);
		w.close();

		assertTrue(bundle.exists());
		assertTrue(bundle.length() > messageBody.length);
	}

	@Test
	public void testWriteAndReadBundle() throws Exception {

		testWriteBundle();

		MessageParser messageParser = new MessageParser() {
			public Message parseMessage(byte[] body) throws FormatException,
			SignatureException {
				// FIXME: Really parse the message
				return message;
			}
		};
		Provider<HeaderBuilder> headerBuilderProvider =
			new Provider<HeaderBuilder>() {
			public HeaderBuilder get() {
				return new IncomingHeaderBuilder(keyPair, sig, digest, wf);
			}
		};
		Provider<BatchBuilder> batchBuilderProvider =
			new Provider<BatchBuilder>() {
			public BatchBuilder get() {
				return new IncomingBatchBuilder(keyPair, sig, digest, wf);
			}
		};

		FileInputStream in = new FileInputStream(bundle);
		Reader reader = new ReaderFactoryImpl().createReader(in);
		BundleReader r = new BundleReaderImpl(reader, bundle.length(),
				messageParser, headerBuilderProvider, batchBuilderProvider);

		Header h = r.getHeader();
		assertEquals(acks, h.getAcks());
		assertEquals(subs, h.getSubscriptions());
		assertEquals(transports, h.getTransports());
		Batch b = r.getNextBatch();
		assertEquals(Collections.singletonList(message), b.getMessages());
		assertNull(r.getNextBatch());
		r.close();
	}

	@After
	public void tearDown() {
		TestUtils.deleteTestDirectory(testDir);
	}
}
