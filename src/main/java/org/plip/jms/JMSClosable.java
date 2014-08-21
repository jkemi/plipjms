package org.plip.jms;

import javax.jms.JMSException;

/**
 * Declares an interface that is closable, but might throw an JMSException
 */
public interface JMSClosable extends AutoCloseable {
	@Override
	void close() throws JMSException;
}
