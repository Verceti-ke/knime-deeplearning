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
package org.knime.dl.keras.core.layers.impl.pooling;

import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.layers.DLConvolutionLayerUtils;
import org.knime.dl.keras.core.layers.DLInputShapeValidationUtils;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractUnaryLayer;
import org.knime.dl.keras.core.layers.DLKerasPadding;
import org.knime.dl.keras.core.layers.DLLayerUtils;
import org.knime.dl.keras.core.layers.DLParameterValidationUtils;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasMaxPooling2DLayer extends DLKerasAbstractUnaryLayer {

    @Parameter(label = "Pool size")
    private DLKerasTuple m_poolSize = new DLKerasTuple("2, 2");

    @Parameter(label = "Strides")
    private DLKerasTuple m_strides = new DLKerasTuple("1, 1");

    @Parameter(label = "Padding")
    private DLKerasPadding m_padding = DLKerasPadding.VALID;

    /**
     * Constructor
     */
    public DLKerasMaxPooling2DLayer() {
        super("keras.layers.MaxPooling2D", DLLayerUtils.NUMERICAL_DTYPES);
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        super.validateParameters();
    }

    @Override
    protected void validateInputShape(final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        DLInputShapeValidationUtils.dimsExactly(inputShape, 3);

        final String message =
            DLParameterValidationUtils.checkPoolingOutputGreaterThanZero(inferOutputShape(inputShape));
        checkInputSpec(message == null, message);
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        return DLConvolutionLayerUtils.computeOutputShape(inputShape, m_poolSize.getTuple(), m_strides.getTuple(),
            DLConvolutionLayerUtils.DEFAULT_2D_DILATION, m_padding.value(), getDataFormat().value());
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        namedParams.put("pool_size", m_poolSize.toPytonTuple());
        namedParams.put("strides", m_strides.toPytonTuple());
        namedParams.put("data_format", DLPythonUtils.toPython(getDataFormat().value()));
        namedParams.put("padding", DLPythonUtils.toPython(m_padding.value()));
    }
}
