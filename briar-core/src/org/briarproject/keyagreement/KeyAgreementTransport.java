package org.briarproject.keyagreement;

import org.briarproject.api.TransportId;
import org.briarproject.api.keyagreement.KeyAgreementConnection;
import org.briarproject.api.plugins.duplex.DuplexTransportConnection;
import org.briarproject.util.ByteUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;
import static org.briarproject.api.keyagreement.KeyAgreementConstants.PROTOCOL_VERSION;
import static org.briarproject.api.keyagreement.KeyAgreementConstants.RECORD_HEADER_LENGTH;
import static org.briarproject.api.keyagreement.KeyAgreementConstants.RECORD_HEADER_PAYLOAD_LENGTH_OFFSET;
import static org.briarproject.api.keyagreement.RecordTypes.ABORT;
import static org.briarproject.api.keyagreement.RecordTypes.CONFIRM;
import static org.briarproject.api.keyagreement.RecordTypes.KEY;

/**
 * Handles the sending and receiving of BQP records.
 */
class KeyAgreementTransport {

	private static final Logger LOG =
			Logger.getLogger(KeyAgreementTransport.class.getName());

	private final KeyAgreementConnection kac;
	private final InputStream in;
	private final OutputStream out;

	public KeyAgreementTransport(KeyAgreementConnection kac)
			throws IOException {
		this.kac = kac;
		in = kac.getConnection().getReader().getInputStream();
		out = kac.getConnection().getWriter().getOutputStream();
	}

	public DuplexTransportConnection getConnection() {
		return kac.getConnection();
	}

	public TransportId getTransportId() {
		return kac.getTransportId();
	}

	public void sendKey(byte[] key) throws IOException {
		writeRecord(KEY, key);
	}

	public byte[] receiveKey() throws AbortException {
		return readRecord(KEY);
	}

	public void sendConfirm(byte[] confirm) throws IOException {
		writeRecord(CONFIRM, confirm);
	}

	public byte[] receiveConfirm() throws AbortException {
		return readRecord(CONFIRM);
	}

	public void sendAbort(boolean exception) {
		try {
			writeRecord(ABORT, new byte[0]);
		} catch (IOException e) {
			if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
			exception = true;
		}
		tryToClose(exception);
	}

	public void tryToClose(boolean exception) {
		try {
			LOG.info("Closing connection");
			kac.getConnection().getReader().dispose(exception, true);
			kac.getConnection().getWriter().dispose(exception);
		} catch (IOException e) {
			if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
		}
	}

	private void writeRecord(byte type, byte[] payload) throws IOException {
		byte[] recordHeader = new byte[RECORD_HEADER_LENGTH];
		recordHeader[0] = PROTOCOL_VERSION;
		recordHeader[1] = type;
		ByteUtils.writeUint16(payload.length, recordHeader,
				RECORD_HEADER_PAYLOAD_LENGTH_OFFSET);
		out.write(recordHeader);
		out.write(payload);
		out.flush();
	}

	private byte[] readRecord(byte type) throws AbortException {
		byte[] header = readHeader();
		if (header[0] != PROTOCOL_VERSION)
			throw new AbortException(); // TODO handle?
		if (header[1] != type) {
			// Unexpected packet
			throw new AbortException(header[1] == ABORT);
		}
		int len = ByteUtils.readUint16(header,
				RECORD_HEADER_PAYLOAD_LENGTH_OFFSET);
		try {
			return readData(len);
		} catch (IOException e) {
			throw new AbortException(e);
		}
	}

	private byte[] readHeader() throws AbortException {
		try {
			return readData(RECORD_HEADER_LENGTH);
		} catch (IOException e) {
			throw new AbortException(e);
		}
	}

	private byte[] readData(int len) throws IOException {
		byte[] data = new byte[len];
		int offset = 0;
		while (offset < data.length) {
			int read = in.read(data, offset, data.length - offset);
			if (read == -1) throw new EOFException();
			offset += read;
		}
		return data;
	}
}