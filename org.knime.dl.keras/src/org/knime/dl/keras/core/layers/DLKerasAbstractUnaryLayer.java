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
package org.knime.dl.keras.core.layers;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.struct.param.Parameter;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractUnaryLayer extends DLKerasAbstractInnerLayer implements DLKerasUnaryLayer {

    // Don't ask 
    @Parameter(label = "Input tensor", min = "0")
    private DLTensorSpec m_spec = null;
    
    private final Set<Class<?>> m_allowedDtypes;

    public DLKerasAbstractUnaryLayer(final String kerasIdentifier, final Set<Class<?>> allowedDtypes) {
        super(kerasIdentifier, 1);
        m_allowedDtypes = allowedDtypes;
    }

    public DLKerasAbstractUnaryLayer(final String kerasIdentifier, final DLKerasLayer parent, final Set<Class<?>> allowedDtypes) {
        super(kerasIdentifier, new DLKerasLayer[]{parent});
        m_allowedDtypes = allowedDtypes;
    }
    
    @Override
    public String populateCall(List<String> parentTensors) {
        if (parentTensors.size() != 1) {
            throw new IllegalArgumentException("An unary layer expects exactly one input tensor.");
        }
        return parentTensors.get(0);
    }

    @Override
    public DLTensorSpec getInputTensorSpec(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException();
        }
        return m_spec;
    }
    
    @Override
    public void setInputTensorSpec(int index, DLTensorSpec inputTensorSpec) {
        if (index != 0) {
            throw new IndexOutOfBoundsException();
        }
        m_spec = inputTensorSpec;
    }

    // Convenience methods:
    protected abstract void validateInputShape(Long[] inputShape)
        throws DLInvalidTensorSpecException;

    protected abstract Long[] inferOutputShape(Long[] inputShape);

    /**
     * The default behavior is to return <code>inputElementType</code>.
     */
    protected Class<?> inferOutputElementType(final Class<?> inputElementType) {
        return inputElementType;
    }

    @Override
    protected final void validateInputSpecs(final List<Class<?>> inputElementTypes, final List<Long[]> inputShapes)
        throws DLInvalidTensorSpecException {
        validateInputType(inputElementTypes.get(0));
        validateInputShape(inputShapes.get(0));
    }
    
    protected void validateInputType(Class<?> inputElementType) throws DLInvalidTensorSpecException {
        if (!m_allowedDtypes.contains(inputElementType)) {
            throw new DLInvalidTensorSpecException("The tensor at port " 
                    + " is of type " + inputElementType + " which is not among the supported types " 
                    + m_allowedDtypes.stream().map(Object::toString).collect(Collectors.joining(", ", "[", "]")));
        }
    }

    @Override
    protected final List<Class<?>> inferOutputElementTypes(final List<Class<?>> inputElementTypes)
        throws DLInvalidTensorSpecException {
        return Collections.singletonList(inferOutputElementType(inputElementTypes.get(0)));
    }

    @Override
    protected final List<Long[]> inferOutputShapes(final List<Long[]> inputShape) {
        return Collections.singletonList(inferOutputShape(inputShape.get(0)));
    }
}
