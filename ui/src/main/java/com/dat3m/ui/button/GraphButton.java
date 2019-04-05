package com.dat3m.ui.button;

import static com.dat3m.dartagnan.wmm.utils.Mode.KNASTER;
import static com.dat3m.ui.options.OptionsPane.OPTWIDTH;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JRadioButton;

import com.dat3m.dartagnan.wmm.utils.Mode;
import com.dat3m.ui.options.utils.ControlCode;

public class GraphButton extends JRadioButton implements ActionListener {

	public GraphButton() {
        super("Draw Graph");
        setActionCommand(ControlCode.GRAPH.actionCommand());
		setMaximumSize(new Dimension(OPTWIDTH/2, 50));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(ControlCode.MODE.actionCommand())){
				setEnabled(!(((JComboBox<Mode>)e.getSource()).getSelectedItem()).equals(KNASTER));				
		}
	}
}
