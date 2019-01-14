/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.dl.core.training;

import java.util.Map;

import org.knime.dl.core.DLAbstractKnimeNetworkInputPreparer;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidNetworkInputException;
import org.knime.dl.core.DLNetworkFixedSizeInputPreparer;
import org.knime.dl.core.DLRowIterator;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractKnimeNetworkFixedSizeInputPreparer extends DLAbstractKnimeNetworkInputPreparer
    implements DLNetworkFixedSizeInputPreparer {

    private long m_currentBatch;

    /**
     * @param iterator provides the input data rows that are used by this instance to prepare (fill) the network tensors
     *            fed to {@link #prepare(Map, long)}.
     * @param batchSize the batch size of the tensors that will be prepared by this instance
     * @param converters the converters that are used to write the data rows into the tensors. The given tensor ids
     *            determine the set of tensors supported by {@link #prepare(Map, long)}.
     */
    public DLAbstractKnimeNetworkFixedSizeInputPreparer(final DLRowIterator iterator, final int batchSize,
        final Map<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> converters) {
        super(iterator, batchSize, converters);
        m_currentBatch = 0;
    }

    @Override
    public boolean hasNext() {
        return m_currentBatch < getNumBatches();
    }

    @Override
    public void prepareNext(final Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input)
        throws DLCanceledExecutionException, DLInvalidNetworkInputException {
        prepare(input, m_currentBatch);
        m_currentBatch++;
    }
}
