@ReactiveBuilder(value = {
        JLabel.class,
        JFrame.class,
        JButton.class,
        JCheckBox.class,
        JPanel.class,
        JTextField.class,
        JComboBox.class,
        JSpinner.class
}, aggregatorClassname = "ReactiveSwing", extensions = SwingExtension.class)
package eu.niton.signaling.swing;

import eu.nitonfx.signaling.processors.elementbuilder.ReactiveBuilder;

import javax.swing.*;