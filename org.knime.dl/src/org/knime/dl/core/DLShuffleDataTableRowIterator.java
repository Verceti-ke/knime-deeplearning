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
package org.knime.dl.core;

import java.util.Map;
import java.util.Random;

import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.sort.ClosableShuffler;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public final class DLShuffleDataTableRowIterator extends DLAbstractDataTableRowIterator {

	private final Random m_random;
	private final ExecutionContext m_exec;
    private ClosableShuffler m_shuffler;

	/**
	 * @param input the data table
	 * @param columns a map specifying which columns belong to which tensor
	 * @param seed seed for random number generator
	 * @param exec execution context necessary for shuffling
	 */
	public DLShuffleDataTableRowIterator(final BufferedDataTable input, final Map<DLTensorId, int[]> columns, final long seed, final ExecutionContext exec) {
		super(input, columns);
		m_random = new Random(seed);
		m_exec = exec;
		m_iterator = makeNewIterator();
	}

	@Override
	protected CloseableRowIterator makeNewIterator() {
		try {
			m_exec.setMessage("Shuffling training data");
			closeShuffler();
			m_shuffler = new ClosableShuffler(getInputTable(), m_exec, m_random.nextLong());
			return m_shuffler.getShuffled().iterator();
		} catch (CanceledExecutionException cee) {
			throw new IllegalStateException("Execution has been canceled while shuffling training data.", cee);
		}
	}

    @Override
    public void close() {
        super.close();
        closeShuffler();
    }

    private void closeShuffler() {
        if (m_shuffler != null) {
            m_shuffler.close();
            m_shuffler = null;
        }
    }
}
