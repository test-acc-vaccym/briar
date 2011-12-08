package net.sf.briar.protocol.batch;

import static net.sf.briar.api.protocol.ProtocolConstants.MAX_PACKET_LENGTH;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.briar.api.ContactId;
import net.sf.briar.api.db.DatabaseComponent;
import net.sf.briar.api.db.DbException;
import net.sf.briar.api.protocol.Ack;
import net.sf.briar.api.protocol.ProtocolWriter;
import net.sf.briar.api.protocol.ProtocolWriterFactory;
import net.sf.briar.api.protocol.RawBatch;
import net.sf.briar.api.protocol.SubscriptionUpdate;
import net.sf.briar.api.protocol.TransportIndex;
import net.sf.briar.api.protocol.TransportUpdate;
import net.sf.briar.api.transport.BatchTransportWriter;
import net.sf.briar.api.transport.ConnectionContext;
import net.sf.briar.api.transport.ConnectionWriter;
import net.sf.briar.api.transport.ConnectionWriterFactory;

class OutgoingBatchConnection {

	private static final Logger LOG =
		Logger.getLogger(OutgoingBatchConnection.class.getName());

	private final DatabaseComponent db;
	private final ConnectionWriterFactory connFactory;
	private final ProtocolWriterFactory protoFactory;
	private final ContactId contactId;
	private final TransportIndex transportIndex;
	private final BatchTransportWriter transport;

	OutgoingBatchConnection(DatabaseComponent db,
			ConnectionWriterFactory connFactory,
			ProtocolWriterFactory protoFactory, ContactId contactId,
			TransportIndex transportIndex, BatchTransportWriter transport) {
		this.db = db;
		this.connFactory = connFactory;
		this.protoFactory = protoFactory;
		this.contactId = contactId;
		this.transportIndex = transportIndex;
		this.transport = transport;
	}

	void write() {
		try {
			ConnectionContext ctx = db.getConnectionContext(contactId,
					transportIndex);
			ConnectionWriter conn = connFactory.createConnectionWriter(
					transport.getOutputStream(), transport.getCapacity(),
					ctx.getSecret());
			OutputStream out = conn.getOutputStream();
			ProtocolWriter writer = protoFactory.createProtocolWriter(out);
			// There should be enough space for a packet
			long capacity = conn.getRemainingCapacity();
			if(capacity < MAX_PACKET_LENGTH) throw new EOFException();
			// Write a transport update
			TransportUpdate t = db.generateTransportUpdate(contactId);
			if(t != null) writer.writeTransportUpdate(t);
			// If there's space, write a subscription update
			capacity = conn.getRemainingCapacity();
			if(capacity >= MAX_PACKET_LENGTH) {
				SubscriptionUpdate s = db.generateSubscriptionUpdate(contactId);
				if(s != null) writer.writeSubscriptionUpdate(s);
			}
			// Write acks until you can't write acks no more
			capacity = conn.getRemainingCapacity();
			int maxBatches = writer.getMaxBatchesForAck(capacity);
			Ack a = db.generateAck(contactId, maxBatches);
			while(a != null) {
				writer.writeAck(a);
				capacity = conn.getRemainingCapacity();
				maxBatches = writer.getMaxBatchesForAck(capacity);
				a = db.generateAck(contactId, maxBatches);
			}
			// Write batches until you can't write batches no more
			capacity = conn.getRemainingCapacity();
			capacity = writer.getMessageCapacityForBatch(capacity);
			RawBatch b = db.generateBatch(contactId, (int) capacity);
			while(b != null) {
				writer.writeBatch(b);
				capacity = conn.getRemainingCapacity();
				capacity = writer.getMessageCapacityForBatch(capacity);
				b = db.generateBatch(contactId, (int) capacity);
			}
			// Flush the output stream
			out.flush();
			transport.dispose(true);
		} catch(DbException e) {
			if(LOG.isLoggable(Level.WARNING)) LOG.warning(e.toString());
			transport.dispose(false);
		} catch(IOException e) {
			if(LOG.isLoggable(Level.WARNING)) LOG.warning(e.toString());
			transport.dispose(false);
		}
	}
}
