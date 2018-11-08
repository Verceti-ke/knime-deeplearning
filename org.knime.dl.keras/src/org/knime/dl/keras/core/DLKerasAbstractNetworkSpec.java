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
package org.knime.dl.keras.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.knime.core.util.Version;
import org.knime.dl.core.DLAbstractNetworkSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.training.DLTrainingConfig;
import org.knime.dl.keras.core.training.DLKerasTrainingConfig;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractNetworkSpec extends DLAbstractNetworkSpec<DLKerasTrainingConfig>
    implements DLKerasNetworkSpec {

    private static final long serialVersionUID = 1L;

    private /** final */ Version m_pythonVersion;

    private /** final */ Version m_kerasVersion;

    /**
     * Creates a new instance of this network spec.
     *
     * @param pythonVersion the Python version of the network
     * @param kerasVersion the Keras version of the network
     * @param inputSpecs the input tensor specs, can be empty
     * @param hiddenOutputSpecs the hidden output tensor specs, can be empty
     * @param outputSpecs the output tensor specs, can be empty
     */
    protected DLKerasAbstractNetworkSpec(final Version pythonVersion, final Version kerasVersion,
        final DLTensorSpec[] inputSpecs, final DLTensorSpec[] hiddenOutputSpecs, final DLTensorSpec[] outputSpecs) {
        super(DLKerasNetworkSpec.getKerasBundleVersion(), inputSpecs, hiddenOutputSpecs, outputSpecs);
        m_pythonVersion = checkNotNull(pythonVersion);
        m_kerasVersion = checkNotNull(kerasVersion);
    }

    /**
     * Creates a new instance of this network spec.
     *
     * @param pythonVersion the Python version of the network
     * @param kerasVersion the Keras version of the network
     * @param inputSpecs the input tensor specs, can be empty
     * @param hiddenOutputSpecs the hidden output tensor specs, can be empty
     * @param outputSpecs the output tensor specs, can be empt
     * @param trainingConfig the {@link DLTrainingConfig training configuration}, must be {@link Serializable}
     */
    protected DLKerasAbstractNetworkSpec(final Version pythonVersion, final Version kerasVersion,
        final DLTensorSpec[] inputSpecs, final DLTensorSpec[] hiddenOutputSpecs, final DLTensorSpec[] outputSpecs,
        final DLKerasTrainingConfig trainingConfig) {
        super(DLKerasNetworkSpec.getKerasBundleVersion(), inputSpecs, hiddenOutputSpecs, outputSpecs, trainingConfig);
        m_pythonVersion = checkNotNull(pythonVersion);
        m_kerasVersion = checkNotNull(kerasVersion);
    }

    /**
     * Creates a new instance of this network spec. And sets the Python and Keras version to null.
     *
     * @param inputSpecs the input tensor specs, can be empty
     * @param hiddenOutputSpecs the hidden output tensor specs, can be empty
     * @param outputSpecs the output tensor specs, can be empty
     */
    protected DLKerasAbstractNetworkSpec(final DLTensorSpec[] inputSpecs, final DLTensorSpec[] hiddenOutputSpecs,
        final DLTensorSpec[] outputSpecs) {
        super(DLKerasNetworkSpec.getKerasBundleVersion(), inputSpecs, hiddenOutputSpecs, outputSpecs);
        m_pythonVersion = null;
        m_kerasVersion = null;
    }

    /**
     * Creates a new instance of this network spec. And sets the Python and Keras version to null.
     *
     * @param inputSpecs the input tensor specs, can be empty
     * @param hiddenOutputSpecs the hidden output tensor specs, can be empty
     * @param outputSpecs the output tensor specs, can be empty
     * @param trainingConfig the {@link DLTrainingConfig training configuration}, must be {@link Serializable}
     */
    protected DLKerasAbstractNetworkSpec(final DLTensorSpec[] inputSpecs, final DLTensorSpec[] hiddenOutputSpecs,
        final DLTensorSpec[] outputSpecs, final DLKerasTrainingConfig trainingConfig) {
        super(DLKerasNetworkSpec.getKerasBundleVersion(), inputSpecs, hiddenOutputSpecs, outputSpecs, trainingConfig);
        m_pythonVersion = null;
        m_kerasVersion = null;
    }

    @Override
    public Version getPythonVersion() {
        return m_pythonVersion;
    }

    @Override
    public Version getKerasVersion() {
        return m_kerasVersion;
    }

    @SuppressWarnings("static-method") // signature must be exactly as is
    private void writeObject(final ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        m_pythonVersion = fixBlankQualifier(m_pythonVersion);
        m_kerasVersion = fixBlankQualifier(m_kerasVersion);
    }

    private static Version fixBlankQualifier(Version version) {
        // Older versions of Version don't have a qualifier.
        // Leaving it blank would lead to an NPE in its compareTo method.
        // Note that even older versions of this spec don't have such Versions at all.
        if (version != null && version.getQualifier() == null) {
            return new Version(version.getMajor(), version.getMinor(), version.getRevision());
        } else {
            return version;
        }
    }
}
