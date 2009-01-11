package context.arch.generator;

//------------------------------------------------------------------------------
// PDialog.java:
//		Implementation for container control initialization class PDialog
//
// WARNING: Do not modify this file.  This file is recreated each time its
//          associated .rct/.res file is sent through the Java Resource Wizard!
//
// This class can be use to create controls within any container, however, the
// following describes how to use this class with an Applet.  For addtional
// information on using Java Resource Wizard generated classes, refer to the
// Visual J++ 1.1 documention.
//
// 1) Import this class in the .java file for the Applet that will use it:
//
//      import PDialog;
//
// 2) Create an instance of this class in your Applet's 'init' member, and call
//    CreateControls() through this object:
//
//      PDialog ctrls = new PDialog (this);
//      ctrls.CreateControls();
//
// 3) To process events generated from user action on these controls, implement
//    the 'handleEvent' member for your applet:
//
//      public boolean handleEvent (Event evt)
//      {
//
//      }
//
//------------------------------------------------------------------------------

import java.awt.*;

public class PDialog {
  Container    m_Parent       = null;
  boolean      m_fInitialized = false;
  DialogLayout m_Layout;

  // Control definitions
  //--------------------------------------------------------------------------
  Button        IDC_GREGORY;
  Button        IDC_ANIND;
  Button        IDC_DANIEL;
  Button        IDC_FUTAKAWA;
  Button        IDC_ISHIGURO;
  Button        IDC_MARIA;

  // Constructor
  //--------------------------------------------------------------------------
  public PDialog (Container parent) {
    m_Parent = parent;
  }

  // Initialization.
  //--------------------------------------------------------------------------
  public boolean CreateControls() {
    // Can only init controls once
    //----------------------------------------------------------------------
    if (m_fInitialized || m_Parent == null) {
      return false;
    }

    // Parent must be a derivation of the Container class
    //----------------------------------------------------------------------
    if (!(m_Parent instanceof Container)) {
      return false;
    }

    // Since there is no way to know if a given font is supported from
    // platform to platform, we only change the size of the font, and not
    // type face name.  And, we only change the font if the dialog resource
    // specified a font.
    //----------------------------------------------------------------------
    Font OldFnt = m_Parent.getFont();
    if (OldFnt != null) {
      Font NewFnt = new Font(OldFnt.getName(), OldFnt.getStyle(), 9);
      m_Parent.setFont(NewFnt);
    }

    // All position and sizes are in Dialog Units, so, we use the
    // DialogLayout manager.
    //----------------------------------------------------------------------
    m_Layout = new DialogLayout(m_Parent, 143, 85);
    m_Parent.setLayout(m_Layout);
    m_Parent.addNotify();

    Dimension size   = m_Layout.getDialogSize();
    Insets    insets = m_Parent.insets();
		
    m_Parent.resize(insets.left + size.width  + insets.right,
                        insets.top  + size.height + insets.bottom);

    // Control creation
    //----------------------------------------------------------------------
    IDC_GREGORY = new Button ("Gregory");
    m_Parent.add(IDC_GREGORY);
    m_Layout.setShape(IDC_GREGORY, 7, 7, 54, 13);

    IDC_ANIND = new Button ("Anind");
    m_Parent.add(IDC_ANIND);
    m_Layout.setShape(IDC_ANIND, 7, 31, 54, 13);

    IDC_DANIEL = new Button ("Daniel");
    m_Parent.add(IDC_DANIEL);
    m_Layout.setShape(IDC_DANIEL, 7, 55, 54, 13);

    IDC_FUTAKAWA = new Button ("Futakawa");
    m_Parent.add(IDC_FUTAKAWA);
    m_Layout.setShape(IDC_FUTAKAWA, 78, 7, 54, 13);

    IDC_ISHIGURO = new Button ("Ishiguro");
    m_Parent.add(IDC_ISHIGURO);
    m_Layout.setShape(IDC_ISHIGURO, 78, 31, 54, 13);

    IDC_MARIA = new Button ("Maria");
    m_Parent.add(IDC_MARIA);
    m_Layout.setShape(IDC_MARIA, 78, 55, 54, 13);

    m_fInitialized = true;
    return true;
  }
}