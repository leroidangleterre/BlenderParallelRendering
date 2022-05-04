package blenderparallelrendering;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author arthu
 */
public class TableButtonRenderer implements TableCellRenderer {

    private JButton button;

    public TableButtonRenderer(JButton b) {
        button = b;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

        button.setForeground(Color.black);
        button.setBackground(UIManager.getColor("Button.background"));
        button.setText((value == null) ? "" : value.toString());
        return button;
    }

}
