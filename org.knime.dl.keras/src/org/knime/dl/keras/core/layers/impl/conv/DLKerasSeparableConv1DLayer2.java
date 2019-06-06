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
package org.knime.dl.keras.core.layers.impl.conv;

import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.config.DLKerasConfigObjectUtils;
import org.knime.dl.keras.core.config.activation.DLKerasActivation;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraint;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraintChoices2;
import org.knime.dl.keras.core.config.initializer.DLKerasGlorotUniformInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializerChoices2;
import org.knime.dl.keras.core.config.initializer.DLKerasZerosInitializer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizerChoices2;
import org.knime.dl.keras.core.layers.DLConvolutionLayerUtils;
import org.knime.dl.keras.core.layers.DLInputShapeValidationUtils;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractUnaryLayer;
import org.knime.dl.keras.core.layers.DLKerasPadding;
import org.knime.dl.keras.core.layers.DLLayerUtils;
import org.knime.dl.keras.core.layers.DLParameterValidationUtils;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.keras.core.struct.param.Required;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasSeparableConv1DLayer2 extends DLKerasAbstractUnaryLayer {

    @Parameter(label = "Filters", min = "1")
    private int m_filters = 1;

    @Parameter(label = "Kernel size", min = "1")
    private int m_kernelSize = 1;

    @Parameter(label = "Strides", min = "1")
    private int m_strides = 1;

    @Parameter(label = "Padding")
    private DLKerasPadding m_padding = DLKerasPadding.VALID;

    @Parameter(label = "Dilation rate", min = "1")
    private int m_dilationRate = 1;

    @Parameter(label = "Depth multiplier", min = "1")
    private int m_depthMultiplier = 1;

    @Parameter(label = "Activation function")
    private DLKerasActivation m_activation = DLKerasActivation.LINEAR;

    @Parameter(label = "Use bias?", tab = "Advanced")
    boolean m_useBias = true;

    @Parameter(label = "Depthwise initializer", choices = DLKerasInitializerChoices2.class, tab = "Advanced")
    private DLKerasInitializer m_depthwiseInitializer = new DLKerasGlorotUniformInitializer();

    @Parameter(label = "Pointwise initializer", choices = DLKerasInitializerChoices2.class, tab = "Advanced")
    private DLKerasInitializer m_pointwiseInitializer = new DLKerasGlorotUniformInitializer();

    @Parameter(label = "Bias initializer", choices = DLKerasInitializerChoices2.class, tab = "Advanced")
    private DLKerasInitializer m_biasInitializer = new DLKerasZerosInitializer();

    @Parameter(label = "Depthwise regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices2.class, tab = "Advanced")
    private DLKerasRegularizer m_depthwiseRegularizer = null;

    @Parameter(label = "Pointwise regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices2.class, tab = "Advanced")
    private DLKerasRegularizer m_pointwiseRegularizer = null;

    @Parameter(label = "Bias regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices2.class, tab = "Advanced")
    private DLKerasRegularizer m_biasRegularizer = null;

    @Parameter(label = "Activity regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices2.class, tab = "Advanced")
    private DLKerasRegularizer m_activityRegularizer = null;

    @Parameter(label = "Depthwise constraint", required = Required.OptionalAndNotEnabled, choices = DLKerasConstraintChoices2.class, tab = "Advanced")
    private DLKerasConstraint m_depthwiseConstraint = null;

    @Parameter(label = "Pointwise constraint", required = Required.OptionalAndNotEnabled, choices = DLKerasConstraintChoices2.class, tab = "Advanced")
    private DLKerasConstraint m_pointwiseConstraint = null;

    @Parameter(label = "Bias constraint", required = Required.OptionalAndNotEnabled, choices = DLKerasConstraintChoices2.class, tab = "Advanced")
    private DLKerasConstraint m_biasConstraint = null;

    /**
     * Constructor
     */
    public DLKerasSeparableConv1DLayer2() {
        super("keras.layers.SeparableConv1D", DLLayerUtils.FLOATING_POINT_DTYPES);
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        super.validateParameters();
        m_depthwiseInitializer.validateParameters();
        m_pointwiseInitializer.validateParameters();
        m_biasInitializer.validateParameters();
        DLLayerUtils.validateOptionalParameter(m_depthwiseRegularizer);
        DLLayerUtils.validateOptionalParameter(m_pointwiseRegularizer);
        DLLayerUtils.validateOptionalParameter(m_biasRegularizer);
        DLLayerUtils.validateOptionalParameter(m_activityRegularizer);
        DLLayerUtils.validateOptionalParameter(m_depthwiseConstraint);
        DLLayerUtils.validateOptionalParameter(m_pointwiseConstraint);
        DLLayerUtils.validateOptionalParameter(m_biasConstraint);
    }

    @Override
    protected void validateInputShape(final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        DLInputShapeValidationUtils.dimsExactly(inputShape, 2);

        final String message =
            DLParameterValidationUtils.checkConvolutionOutputGreaterThanZero(inferOutputShape(inputShape));
        checkInputSpec(message == null, message);
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        final Long[] kernelSize = {new Long(m_kernelSize)};
        final Long[] strides = {new Long(m_strides)};
        final Long[] dilationRate = {new Long(m_dilationRate)};
        return DLConvolutionLayerUtils.computeOutputShape(inputShape, m_filters, kernelSize, strides, dilationRate,
            m_padding.value(), getDataFormat().value());
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        namedParams.put("filters", DLPythonUtils.toPython(m_filters));
        namedParams.put("kernel_size", DLPythonUtils.toPython(m_kernelSize));
        namedParams.put("strides", DLPythonUtils.toPython(m_strides));
        namedParams.put("padding", DLPythonUtils.toPython(m_padding.value()));
        namedParams.put("data_format", DLPythonUtils.toPython(getDataFormat().value()));
        namedParams.put("dilation_rate", DLPythonUtils.toPython(m_dilationRate));
        namedParams.put("depth_multiplier", DLPythonUtils.toPython(m_depthMultiplier));
        namedParams.put("activation", DLPythonUtils.toPython(m_activation.value()));
        namedParams.put("use_bias", DLPythonUtils.toPython(m_useBias));
        namedParams.put("depthwise_initializer", DLKerasConfigObjectUtils.toPython(m_depthwiseInitializer));
        namedParams.put("pointwise_initializer", DLKerasConfigObjectUtils.toPython(m_pointwiseInitializer));
        namedParams.put("bias_initializer", DLKerasConfigObjectUtils.toPython(m_biasInitializer));
        namedParams.put("depthwise_regularizer", DLKerasConfigObjectUtils.toPython(m_depthwiseRegularizer));
        namedParams.put("pointwise_regularizer", DLKerasConfigObjectUtils.toPython(m_pointwiseRegularizer));
        namedParams.put("bias_regularizer", DLKerasConfigObjectUtils.toPython(m_biasRegularizer));
        namedParams.put("activity_regularizer", DLKerasConfigObjectUtils.toPython(m_activityRegularizer));
        namedParams.put("depthwise_constraint", DLKerasConfigObjectUtils.toPython(m_depthwiseConstraint));
        namedParams.put("pointwise_constraint", DLKerasConfigObjectUtils.toPython(m_pointwiseConstraint));
        namedParams.put("bias_constraint", DLKerasConfigObjectUtils.toPython(m_biasConstraint));
    }
}
