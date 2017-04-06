/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * History
 *   Jun 2, 2017 (marcel): created
 */
package org.knime.dl.base.nodes.executor;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.dl.base.DLGeneralModelConfig;
import org.knime.dl.base.DLInputLayerDataModelConfig;
import org.knime.dl.base.DLOutputLayerDataModelConfig;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.backend.DLBackendRegistry;
import org.knime.dl.core.backend.DLProfile;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
final class DLExecutorNodeDialog extends NodeDialogPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLExecutorNodeModel.class);

    private DLGeneralModelConfig m_generalCfg;

    private ArrayList<DLInputLayerDataPanel> m_inputPanels;

    private LinkedHashMap<String, DLOutputLayerDataPanel> m_outputPanels;

    private JPanel m_root;

    private GridBagConstraints m_rootConstr;

    private JScrollPane m_rootScrollableView;

    private JButton m_outputsAddBtn;

    /**
     * Creates a new dialog.
     */
    public DLExecutorNodeDialog() {
        resetSettings();
        addTab("Options", m_rootScrollableView);
    }

    private void resetSettings() {
        m_generalCfg = DLExecutorNodeModel.createGeneralModelConfig();
        m_inputPanels = new ArrayList<>();
        m_outputPanels = new LinkedHashMap<>();
        // root panel; content will be generated based on input specs
        m_root = new JPanel(new GridBagLayout());
        m_rootConstr = new GridBagConstraints();
        resetDialog();
        m_rootScrollableView = new JScrollPane();
        final JPanel rootWrapper = new JPanel(new BorderLayout());
        rootWrapper.add(m_root, BorderLayout.NORTH);
        m_rootScrollableView.setViewportView(rootWrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        if (specs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX] == null) {
            throw new NotConfigurableException("Input deep learning network port object is missing.");
        }
        if ((DataTableSpec)specs[DLExecutorNodeModel.IN_DATA_PORT_IDX] == null) {
            throw new NotConfigurableException("Inpu data table is missing.");
        }
        if (!DLNetworkPortObject.TYPE.acceptsPortObjectSpec(specs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX])) {
            throw new NotConfigurableException("Input port object is not a valid deep learning network port object.");
        }
        final DLNetworkPortObjectSpec currNetworkSpec =
            (DLNetworkPortObjectSpec)specs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX];
        final DataTableSpec currTableSpec = (DataTableSpec)specs[DLExecutorNodeModel.IN_DATA_PORT_IDX];

        if (currNetworkSpec.getNetworkSpec() == null) {
            throw new NotConfigurableException("Input port object's deep learning network specs are missing.");
        }
        if (currNetworkSpec.getProfile() == null) {
            throw new NotConfigurableException("Input port object's deep learning profile is missing.");
        }
        final DLNetworkSpec networkSpec = currNetworkSpec.getNetworkSpec();
        final DLProfile profile = currNetworkSpec.getProfile();

        if (currTableSpec.getNumColumns() == 0) {
            LOGGER.warn("Input table is empty.");
        }
        if (networkSpec.getInputSpecs().length == 0) {
            LOGGER.warn("Input deep learning network has no input specs.");
        }
        if (networkSpec.getOutputSpecs().length == 0 && networkSpec.getIntermediateOutputSpecs().length == 0) {
            LOGGER.warn("Input deep learning network has no output specs.");
        }
        if (profile.size() == 0) {
            throw new NotConfigurableException("Input deep learning network has no associated back end.");
        }

        resetDialog();
        createDialogContent(currNetworkSpec, currTableSpec);

        // in case we have the same spec...
        // load general settings
        try {
            m_generalCfg.loadFromSettings(settings);
        } catch (final InvalidSettingsException e) {
            // default settings
        }
        try {
            if (settings.containsKey(DLExecutorNodeModel.CFG_KEY_INPUTS)) {
                final NodeSettings tmp =
                    DLExecutorNodeModel.loadFromBytesArray(settings.getNodeSettings(DLExecutorNodeModel.CFG_KEY_INPUTS),
                        DLExecutorNodeModel.CFG_KEY_INPUT_ARRAY);
                for (final DLInputLayerDataPanel inputPanel : m_inputPanels) {
                    if (tmp.containsKey(inputPanel.m_cfg.getInputLayerDataName())) {
                        inputPanel.loadFromSettings(tmp.getNodeSettings(inputPanel.m_cfg.getInputLayerDataName()),
                            specs);
                    } else {
                        inputPanel.m_dcInputColumns.loadConfiguration(inputPanel.m_cfg.getInputColumnsModel(),
                            currTableSpec);
                        inputPanel.m_dcInputColumns.updateWithNewConfiguration(inputPanel.m_cfg.getInputColumnsModel());
                    }
                }
            }
            // load saved output settings if any
            final NodeSettings tmp =
                DLExecutorNodeModel.loadFromBytesArray(settings.getNodeSettings(DLExecutorNodeModel.CFG_KEY_OUTPUTS),
                    DLExecutorNodeModel.CFG_KEY_OUTPUTS_ARRAY);

            for (final String key : tmp) {
                if (!m_outputPanels.containsKey(key)) {
                    // add output to the dialog (when loading the dialog for the
                    // first time)
                    Optional<DLLayerDataSpec> spec =
                        Arrays.stream(networkSpec.getOutputSpecs()).filter(s -> s.getName().equals(key)).findFirst();
                    if (!spec.isPresent()) {
                        spec = Arrays.stream(networkSpec.getIntermediateOutputSpecs())
                            .filter(s -> s.getName().equals(key)).findFirst();
                    }
                    if (spec.isPresent()) {
                        m_outputPanels.put(key, createOutputPanel(spec.get(), m_generalCfg.getBackendModel()));
                    }
                    // TODO: disable 'add output' button when all available
                    // outputs
                    // were loaded
                }
            }

            for (final DLOutputLayerDataPanel outputPanel : m_outputPanels.values()) {
                m_outputPanels.get(outputPanel.m_cfg.getOutputLayerDataName()).loadFromSettings(tmp, specs);
            }

        } catch (final Exception e) {
            m_outputPanels.clear();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_generalCfg.saveToSettings(settings);

        final NodeSettingsWO inputSettings = settings.addNodeSettings(DLExecutorNodeModel.CFG_KEY_INPUTS);
        NodeSettings tmp = new NodeSettings("tmp");
        for (final DLInputLayerDataPanel inputPanel : m_inputPanels) {
            final NodeSettingsWO child = tmp.addNodeSettings(inputPanel.m_cfg.getInputLayerDataName());
            inputPanel.saveToSettings(child);
        }
        DLExecutorNodeModel.saveAsBytesArray(tmp, inputSettings, DLExecutorNodeModel.CFG_KEY_INPUT_ARRAY);

        final NodeSettingsWO outputSettings = settings.addNodeSettings(DLExecutorNodeModel.CFG_KEY_OUTPUTS);
        tmp = new NodeSettings("tmp");
        for (final DLOutputLayerDataPanel outputPanel : m_outputPanels.values()) {
            final NodeSettingsWO child = tmp.addNodeSettings(outputPanel.m_cfg.getOutputLayerDataName());
            outputPanel.saveToSettings(child);
        }

        DLExecutorNodeModel.saveAsBytesArray(tmp, outputSettings, DLExecutorNodeModel.CFG_KEY_OUTPUTS_ARRAY);

        final SettingsModelStringArray outputOrder =
            DLExecutorNodeModel.createOutputOrderSettingsModel(m_outputPanels.size());
        final String[] outputs = new String[m_outputPanels.size()];

        int i = 0;
        for (final String output : m_outputPanels.keySet()) {
            outputs[i++] = output;
        }
        outputOrder.setStringArrayValue(outputs);
        outputOrder.saveSettingsTo(settings);
    }

    private void resetDialog() {
        m_inputPanels.clear();
        m_outputPanels.clear();
        m_root.removeAll();
        m_rootConstr.gridx = 0;
        m_rootConstr.gridy = 0;
        m_rootConstr.gridwidth = 1;
        m_rootConstr.gridheight = 1;
        m_rootConstr.weightx = 1;
        m_rootConstr.weighty = 0;
        m_rootConstr.anchor = GridBagConstraints.WEST;
        m_rootConstr.fill = GridBagConstraints.BOTH;
        m_rootConstr.insets = new Insets(5, 5, 5, 5);
        m_rootConstr.ipadx = 0;
        m_rootConstr.ipady = 0;
    }

    private void createDialogContent(final DLNetworkPortObjectSpec portObjectSpec, final DataTableSpec tableSpec) {
        final DLNetworkSpec networkSpec = portObjectSpec.getNetworkSpec();
        final DLProfile profile = portObjectSpec.getProfile();

        // general settings:
        final JPanel generalPanel = new JPanel(new GridBagLayout());
        generalPanel.setBorder(BorderFactory.createTitledBorder("General Settings"));
        final GridBagConstraints generalPanelConstr = new GridBagConstraints();
        generalPanelConstr.gridx = 0;
        generalPanelConstr.gridy = 0;
        generalPanelConstr.weightx = 1;
        generalPanelConstr.anchor = GridBagConstraints.WEST;
        generalPanelConstr.fill = GridBagConstraints.VERTICAL;
        // settings model
        m_generalCfg.getBackendModel().setStringValue(DLBackendRegistry.getPreferredBackend(profile).getIdentifier());
        // back end selection
        final DialogComponentStringSelection dcBackend = new DialogComponentStringSelection(
            m_generalCfg.getBackendModel(), "Back end", getAvailableBackends(profile));
        generalPanel.add(dcBackend.getComponentPanel(), generalPanelConstr);
        generalPanelConstr.gridy++;
        // batch size input
        final DialogComponentNumber cdBatchSize =
            new DialogComponentNumber(m_generalCfg.getBatchSizeModel(), "Input batch size", 20);
        generalPanel.add(cdBatchSize.getComponentPanel(), generalPanelConstr);
        generalPanelConstr.gridy++;
        final DialogComponentBoolean appendColumnComponent =
            new DialogComponentBoolean(m_generalCfg.getAppendColumns(), "Keep input columns");
        generalPanel.add(appendColumnComponent.getComponentPanel(), generalPanelConstr);
        m_root.add(generalPanel, m_rootConstr);
        m_rootConstr.gridy++;

        // input settings:
        // separator
        final JPanel inputsSeparator = new JPanel(new GridBagLayout());
        final GridBagConstraints inputsSeparatorLabelConstr = new GridBagConstraints();
        inputsSeparatorLabelConstr.gridwidth = 1;
        inputsSeparatorLabelConstr.weightx = 0;
        inputsSeparatorLabelConstr.anchor = GridBagConstraints.WEST;
        inputsSeparatorLabelConstr.fill = GridBagConstraints.NONE;
        inputsSeparatorLabelConstr.insets = new Insets(7, 7, 7, 7);
        final GridBagConstraints inputsSeparatorSeparatorConstr = new GridBagConstraints();
        inputsSeparatorSeparatorConstr.gridwidth = GridBagConstraints.REMAINDER;
        inputsSeparatorSeparatorConstr.weightx = 1;
        inputsSeparatorSeparatorConstr.fill = GridBagConstraints.HORIZONTAL;
        inputsSeparator.add(new JLabel("Inputs"), inputsSeparatorLabelConstr);
        inputsSeparator.add(new JSeparator(), inputsSeparatorSeparatorConstr);
        m_root.add(inputsSeparator, m_rootConstr);
        m_rootConstr.gridy++;
        // inputs
        for (final DLLayerDataSpec inputDataSpec : networkSpec.getInputSpecs()) {
            createInputPanel(inputDataSpec, tableSpec, m_generalCfg.getBackendModel());
        }

        // output settings:
        // separator
        final JPanel outputsSeparator = new JPanel(new GridBagLayout());
        final GridBagConstraints outputsSeparatorLabelConstr = new GridBagConstraints();
        outputsSeparatorLabelConstr.gridwidth = 1;
        outputsSeparatorLabelConstr.weightx = 0;
        outputsSeparatorLabelConstr.anchor = GridBagConstraints.WEST;
        outputsSeparatorLabelConstr.fill = GridBagConstraints.NONE;
        outputsSeparatorLabelConstr.insets = new Insets(7, 7, 7, 7);
        final GridBagConstraints outputsSeparatorSeparatorConstr = new GridBagConstraints();
        outputsSeparatorSeparatorConstr.gridwidth = GridBagConstraints.REMAINDER;
        outputsSeparatorSeparatorConstr.weightx = 1;
        outputsSeparatorSeparatorConstr.fill = GridBagConstraints.HORIZONTAL;
        outputsSeparator.add(new JLabel("Outputs"), outputsSeparatorLabelConstr);
        outputsSeparator.add(new JSeparator(), outputsSeparatorSeparatorConstr);
        m_root.add(outputsSeparator, m_rootConstr);
        m_rootConstr.gridy++;
        // 'add output' button
        m_outputsAddBtn = new JButton("add output");
        // 'add output' button click event: open dialog
        m_outputsAddBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                // 'add output' dialog
                final JPanel outputsAddDlg = new JPanel(new GridBagLayout());
                final GridBagConstraints addOutputDialogConstr = new GridBagConstraints();
                addOutputDialogConstr.gridx = 0;
                addOutputDialogConstr.gridy = 0;
                addOutputDialogConstr.weightx = 1;
                addOutputDialogConstr.anchor = GridBagConstraints.WEST;
                addOutputDialogConstr.fill = GridBagConstraints.VERTICAL;
                // available outputs
                final ArrayList<String> availableOutputs = new ArrayList<>(networkSpec.getOutputSpecs().length
                    + networkSpec.getIntermediateOutputSpecs().length - m_outputPanels.size());
                final HashMap<String, DLLayerDataSpec> availableOutputsMap = new HashMap<>(availableOutputs.size());
                for (final DLLayerDataSpec outputSpec : networkSpec.getOutputSpecs()) {
                    final String outputName = outputSpec.getName();
                    if (!m_outputPanels.containsKey(outputName)) {
                        availableOutputs.add(outputName);
                        availableOutputsMap.put(outputName, outputSpec);
                    }
                }
                for (int i = networkSpec.getIntermediateOutputSpecs().length - 1; i >= 0; i--) {
                    final DLLayerDataSpec intermediateSpec = networkSpec.getIntermediateOutputSpecs()[i];
                    final String intermediateName = intermediateSpec.getName();
                    if (!m_outputPanels.containsKey(intermediateName)) {
                        final String intermediateDisplayName = intermediateName + " (hidden)";
                        availableOutputs.add(intermediateDisplayName);
                        availableOutputsMap.put(intermediateDisplayName, intermediateSpec);
                    }
                }
                // output selection
                final SettingsModelString smOutput = new SettingsModelString("output", availableOutputs.get(0));
                final DialogComponentStringSelection dcOutput =
                    new DialogComponentStringSelection(smOutput, "Output", availableOutputs);
                outputsAddDlg.add(dcOutput.getComponentPanel(), addOutputDialogConstr);
                final int selectedOption = JOptionPane.showConfirmDialog(DLExecutorNodeDialog.this.getPanel(),
                    outputsAddDlg, "Add output...", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (selectedOption == JOptionPane.OK_OPTION) {
                    final DLLayerDataSpec outputDataSpec = availableOutputsMap.get(smOutput.getStringValue());
                    createOutputPanel(outputDataSpec, m_generalCfg.getBackendModel());
                    if (availableOutputs.size() == 1) {
                        // no more available outputs
                        m_outputsAddBtn.setEnabled(false);
                    }
                }
            }
        });
        final GridBagConstraints outputsAddBtnConstr = (GridBagConstraints)m_rootConstr.clone();
        outputsAddBtnConstr.weightx = 1;
        outputsAddBtnConstr.anchor = GridBagConstraints.EAST;
        outputsAddBtnConstr.fill = GridBagConstraints.NONE;
        outputsAddBtnConstr.insets = new Insets(0, 5, 10, 5);
        m_root.add(m_outputsAddBtn, outputsAddBtnConstr);
        m_rootConstr.gridy++;
    }

    private DLInputLayerDataPanel createInputPanel(final DLLayerDataSpec inputDataSpec, final DataTableSpec tableSpec,
        final SettingsModelString smBackend) {
        // config
        final DLInputLayerDataModelConfig inputCfg =
            DLExecutorNodeModel.createInputLayerDataModelConfig(inputDataSpec.getName(), smBackend);
        // panel
        final DLInputLayerDataPanel inputPanel = new DLInputLayerDataPanel(inputDataSpec, inputCfg, tableSpec);
        addInput(inputPanel);
        return inputPanel;
    }

    private DLOutputLayerDataPanel createOutputPanel(final DLLayerDataSpec outputDataSpec,
        final SettingsModelString smBackend) {
        // config
        final DLOutputLayerDataModelConfig outputCfg =
            DLExecutorNodeModel.createOutputLayerDataModelConfig(outputDataSpec.getName(), smBackend);
        // panel
        final DLOutputLayerDataPanel outputPanel = new DLOutputLayerDataPanel(outputDataSpec, outputCfg);
        outputPanel.addRemoveListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                removeOutput(outputDataSpec.getName(), outputPanel, outputCfg);
            }
        });
        addOutput(outputDataSpec.getName(), outputPanel);
        return outputPanel;
    }

    private void addInput(final DLInputLayerDataPanel inputPanel) {
        m_inputPanels.add(inputPanel);
        m_root.add(inputPanel, m_rootConstr);
        m_rootConstr.gridy++;
    }

    private void addOutput(final String outputName, final DLOutputLayerDataPanel outputPanel) {
        if (!m_outputPanels.containsKey(outputName)) {
            m_outputPanels.put(outputName, outputPanel);
            m_root.add(outputPanel, m_rootConstr);
            m_rootConstr.gridy++;
            m_rootScrollableView.validate();
            final JScrollBar scrollBar = m_rootScrollableView.getVerticalScrollBar();
            scrollBar.setValue(scrollBar.getMaximum());
            m_rootScrollableView.repaint();
        }
    }

    private void removeOutput(final String outputName, final JPanel outputPanel,
        final DLOutputLayerDataModelConfig m_cfg) {
        if (m_outputPanels.remove(outputName) != null) {
            m_root.remove(outputPanel);
            m_outputsAddBtn.setEnabled(true);
            m_rootScrollableView.validate();
            m_rootScrollableView.repaint();
        }
    }

    private List<String> getAvailableBackends(final DLProfile profile) {
        final ArrayList<String> backends = new ArrayList<>(profile.size());
        profile.forEach(b -> backends.add(b.getIdentifier()));
        backends.sort(Comparator.naturalOrder());
        return backends;
    }

    // Input/output panels:

    @SuppressWarnings("serial")
    private final class DLInputLayerDataPanel extends JPanel {

        private final DLLayerDataSpec m_inputDataSpec;

        private final DLInputLayerDataModelConfig m_cfg;

        private final DataColumnSpecFilterPanel m_dcInputColumns;

        private final DataTableSpec m_lastTableSpec;

        private final GridBagConstraints m_constr;

        private DLInputLayerDataPanel(final DLLayerDataSpec inputDataSpec, final DLInputLayerDataModelConfig cfg,
            final DataTableSpec tableSpec) {
            super(new GridBagLayout());
            m_inputDataSpec = inputDataSpec;
            m_cfg = cfg;
            m_lastTableSpec = tableSpec;
            m_dcInputColumns = new DataColumnSpecFilterPanel(DataValue.class);
            m_constr = new GridBagConstraints();
            m_constr.gridx = 0;
            m_constr.gridy = 0;
            m_constr.weightx = 1;
            m_constr.anchor = GridBagConstraints.WEST;
            m_constr.fill = GridBagConstraints.VERTICAL;
            constructPanel();
        }

        private void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws InvalidSettingsException, NotConfigurableException {
            m_cfg.loadFromSettings(settings);
            m_dcInputColumns.loadConfiguration(m_cfg.getInputColumnsModel(), m_lastTableSpec);
            m_dcInputColumns.updateWithNewConfiguration(m_cfg.getInputColumnsModel());
        }

        private void saveToSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
            m_dcInputColumns.saveConfiguration(m_cfg.getInputColumnsModel());
            m_cfg.saveToSettings(settings);
            final FilterResult filter = m_cfg.getInputColumnsModel().applyTo(m_lastTableSpec);
            if (filter.getIncludes().length > DLUtils.Shapes.getSize(m_inputDataSpec.getShape())) {
                throw new InvalidSettingsException("More columns selected than input neurons available!");
            }

            DataType type = null;
            for (final String include : filter.getIncludes()) {
                if (type == null) {
                    type = m_lastTableSpec.getColumnSpec(include).getType();
                } else {
                    if (!m_lastTableSpec.getColumnSpec(include).getType().equals(type)) {
                        throw new InvalidSettingsException(
                            "Only columns of same type can be converted into a single layer.");
                    }
                }
            }
        }

        private void constructPanel() {
            setBorder(BorderFactory.createTitledBorder("Input: " + m_inputDataSpec.getName()));
            // meta information:
            final JPanel numNeurons = new JPanel();
            final GridBagConstraints numNeuronsConstr = new GridBagConstraints();
            numNeuronsConstr.insets = new Insets(5, 0, 5, 0);
            numNeurons.add(new JLabel("Number of neurons: " + DLUtils.Shapes.getSize(m_inputDataSpec.getShape())),
                numNeuronsConstr);
            add(numNeurons, m_constr);
            m_constr.gridy++;
            final JPanel shape = new JPanel();
            final GridBagConstraints shapeConstr = new GridBagConstraints();
            shapeConstr.insets = new Insets(5, 0, 5, 0);
            shape.add(new JLabel("Shape: " + m_inputDataSpec.shapeToString()), shapeConstr);
            add(shape, m_constr);
            m_constr.gridy++;
            // converter selection
            List<String> availableConverters = m_cfg.getAvailableConverters(m_lastTableSpec, m_inputDataSpec);
            if (availableConverters.size() == 0) {
                LOGGER.error("No converters available for input '" + m_inputDataSpec.getName() + "'.");
                availableConverters = Arrays.asList("<none>");
            }
            final DialogComponentStringSelection dcConverter =
                new DialogComponentStringSelection(m_cfg.getConverterModel(), "Converter", availableConverters);
            m_cfg.addAvailableConvertersChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(final ChangeEvent e) {
                    // update converter selection list. Try to preserve the
                    // selected converter (e.g. when a different back
                    // end has a corresponding converter with equal name).
                    // TODO: also check for equal specs and search for a
                    // converter that preserves the specs if the check fails
                    final SettingsModelString smConverter = m_cfg.getConverterModel();
                    final String oldConverter = smConverter.getStringValue();
                    try {
                        final List<String> newConverters =
                            m_cfg.getAvailableConverters(m_lastTableSpec, m_inputDataSpec);
                        final String newConverter = newConverters.contains(oldConverter) ? oldConverter
                            : m_cfg.getPreferredConverter(m_lastTableSpec, m_inputDataSpec);
                        smConverter.setStringValue(newConverter);
                        dcConverter.replaceListItems(newConverters, newConverter);
                    } catch (final Exception ex) {
                        LOGGER.error("No converters available for input '" + m_inputDataSpec.getName() + "'.");
                    }
                }
            });
            add(dcConverter.getComponentPanel(), m_constr);
            m_constr.gridy++;
            // column selection
            final DataColumnSpecFilterPanel dcInputColumns = m_dcInputColumns;

            m_cfg.addAllowedInputTypesChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(final ChangeEvent e) {
                    // update column selection list
                    // final SettingsModelColumnFilter2 smInputColumns =
                    // m_cfg.getInputColumnsModel();
                    // final Collection<Class<? extends DataValue>>
                    // allowedInputTypes = m_cfg.getAllowedInputTypes();
                    // TODO: update filter
                }
            });
            final JPanel inputColumnsLabel = new JPanel();
            inputColumnsLabel.add(new JLabel("Input columns:"));
            add(inputColumnsLabel, m_constr);
            m_constr.gridy++;
            final JPanel inputColumnsFilter = new JPanel();
            inputColumnsFilter.add(dcInputColumns);
            add(inputColumnsFilter, m_constr);
            m_constr.gridy++;
        }
    }

    @SuppressWarnings("serial")
    private static final class DLOutputLayerDataPanel extends JPanel {

        private final DLLayerDataSpec m_outputDataSpec;

        private final DLOutputLayerDataModelConfig m_cfg;

        private final GridBagConstraints m_constr;

        private final CopyOnWriteArrayList<ChangeListener> m_removeListeners;

        private DialogComponentStringSelection m_dcConverter;

        private DLOutputLayerDataPanel(final DLLayerDataSpec outputDataSpec, final DLOutputLayerDataModelConfig cfg) {
            super(new GridBagLayout());
            m_outputDataSpec = outputDataSpec;
            m_cfg = cfg;
            m_constr = new GridBagConstraints();
            m_removeListeners = new CopyOnWriteArrayList<>();
            m_constr.gridx = 0;
            m_constr.gridy = 0;
            m_constr.weightx = 1;
            m_constr.anchor = GridBagConstraints.WEST;
            m_constr.fill = GridBagConstraints.VERTICAL;
            constructPanel();
        }

        private void addRemoveListener(final ChangeListener l) {
            if (!m_removeListeners.contains(l)) {
                m_removeListeners.add(l);
            }
        }

        @SuppressWarnings("unused")
        private void removeRemoveListener(final ChangeListener l) {
            m_removeListeners.remove(l);
        }

        private void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
            try {
                m_cfg.loadFromSettings(settings);
                m_dcConverter.loadSettingsFrom(settings, specs);
                m_dcConverter.replaceListItems(m_cfg.getAvailableConverters(m_outputDataSpec),
                    m_cfg.getOutputLayerDataName());
            } catch (final InvalidSettingsException e) {
                // default settings
            }
        }

        private void saveToSettings(final NodeSettingsWO settings) {
            m_cfg.saveToSettings(settings);
        }

        private void constructPanel() {
            setBorder(BorderFactory.createTitledBorder("Output: " + m_outputDataSpec.getName()));
            // meta information:
            final JPanel shape = new JPanel();
            final GridBagConstraints shapeConstr = new GridBagConstraints();
            shapeConstr.insets = new Insets(5, 0, 5, 0);
            shape.add(new JLabel("Shape: " + m_outputDataSpec.shapeToString()), shapeConstr);
            add(shape, m_constr);
            // 'remove' button, see bottom for click event handling
            final JButton outputRemoveBtn = new JButton("remove");
            final GridBagConstraints outputRemoveBtnConstr = (GridBagConstraints)m_constr.clone();
            outputRemoveBtnConstr.weightx = 1;
            outputRemoveBtnConstr.anchor = GridBagConstraints.EAST;
            outputRemoveBtnConstr.fill = GridBagConstraints.NONE;
            outputRemoveBtnConstr.insets = new Insets(0, 0, 0, 5);
            add(outputRemoveBtn, outputRemoveBtnConstr);
            m_constr.gridy++;

            // converter selection
            m_dcConverter = new DialogComponentStringSelection(m_cfg.getConverterModel(), "Converter",
                m_cfg.getAvailableConverters(m_outputDataSpec));
            m_cfg.addAvailableConvertersChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(final ChangeEvent e) {
                    // update converter selection list. Try to preserve the
                    // selected converter (e.g. when a different back
                    // end has a corresponding converter with equal name).
                    // TODO: also check for equal specs and search for a
                    // converter that preserves the specs if the check fails
                    final SettingsModelString smConverter = m_cfg.getConverterModel();
                    final String oldConverter = smConverter.getStringValue();
                    final List<String> newConverters = m_cfg.getAvailableConverters(m_outputDataSpec);
                    final String newConverter = newConverters.contains(oldConverter) ? oldConverter
                        : m_cfg.getPreferredConverter(m_outputDataSpec);
                    smConverter.setStringValue(newConverter);
                    m_dcConverter.replaceListItems(newConverters, newConverter);
                }
            });
            add(m_dcConverter.getComponentPanel(), m_constr);
            m_constr.gridy++;
            // prefix input
            final DialogComponentString dcPrefix =
                new DialogComponentString(m_cfg.getPrefixModel(), "Output columns prefix");
            add(dcPrefix.getComponentPanel(), m_constr);
            m_constr.gridy++;
            // 'remove' button click event: remove output
            outputRemoveBtn.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent e) {
                    onRemove();
                }
            });
        }

        private void onRemove() {
            for (final ChangeListener l : m_removeListeners) {
                l.stateChanged(new ChangeEvent(this));
            }
        }
    }
}
