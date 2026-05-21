import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ClienteFrame extends JFrame {
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    
    // Componentes UI
    private JTextField txtServidor, txtPuerto, txtMensaje;
    private JTextArea areaChat;
    private JButton btnConectar, btnEnviar, btnDesconectar;
    private JLabel lblEstado;
    private boolean conectado = false;

    public ClienteFrame() {
        // Configurar ventana
        setTitle("Cliente TCP - Java Swing");
        setSize(450, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Panels
        JPanel panelConexion = new JPanel(new GridLayout(2, 3, 10, 5));
        JPanel panelMensaje = new JPanel(new BorderLayout(5, 5));
        
        // Panel de conexión
        panelConexion.setBorder(BorderFactory.createTitledBorder("Conexión"));
        
        panelConexion.add(new JLabel("Servidor:"));
        txtServidor = new JTextField("localhost", 12);
        panelConexion.add(txtServidor);
        
        panelConexion.add(new JLabel("Puerto:"));
        txtPuerto = new JTextField("9000", 8);
        panelConexion.add(txtPuerto);
        
        btnConectar = new JButton("Conectar");
        btnConectar.setBackground(new Color(33, 150, 243));
        btnConectar.setForeground(Color.WHITE);
        panelConexion.add(btnConectar);
        
        btnDesconectar = new JButton("Desconectar");
        btnDesconectar.setBackground(new Color(244, 67, 54));
        btnDesconectar.setForeground(Color.WHITE);
        btnDesconectar.setEnabled(false);
        panelConexion.add(btnDesconectar);
        
        lblEstado = new JLabel("Desconectado");
        lblEstado.setForeground(Color.RED);
        panelConexion.add(lblEstado);

        // Panel de mensaje
        panelMensaje.setBorder(BorderFactory.createTitledBorder("Mensajes"));
        
        txtMensaje = new JTextField();
        txtMensaje.setEnabled(false);
        
        btnEnviar = new JButton("Enviar");
        btnEnviar.setBackground(new Color(76, 175, 80));
        btnEnviar.setForeground(Color.WHITE);
        btnEnviar.setEnabled(false);
        
        panelMensaje.add(txtMensaje, BorderLayout.CENTER);
        panelMensaje.add(btnEnviar, BorderLayout.EAST);

        // Área de chat
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollChat = new JScrollPane(areaChat);

        // Layout principal
        setLayout(new BorderLayout(10, 10));
        add(panelConexion, BorderLayout.NORTH);
        add(scrollChat, BorderLayout.CENTER);
        add(panelMensaje, BorderLayout.SOUTH);

        // Eventos
        btnConectar.addActionListener(e -> conectar());
        btnDesconectar.addActionListener(e -> desconectar());
        btnEnviar.addActionListener(e -> enviarMensaje());
        
        // Enviar con Enter
        txtMensaje.addActionListener(e -> {
            if (conectado) enviarMensaje();
        });

        // Cerrar ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                desconectar();
            }
        });
    }

    private void conectar() {
        try {
            String servidor = txtServidor.getText().trim();
            int puerto = Integer.parseInt(txtPuerto.getText().trim());
            
            if (servidor.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Ingrese la dirección del servidor", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Crear socket
            socket = new Socket(servidor, puerto);
            
            // Configurar flujos
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
            
            // Actualizar UI
            conectado = true;
            btnConectar.setEnabled(false);
            btnDesconectar.setEnabled(true);
            btnEnviar.setEnabled(true);
            txtMensaje.setEnabled(true);
            txtServidor.setEditable(false);
            txtPuerto.setEditable(false);
            txtMensaje.requestFocus();
            
            lblEstado.setText("Conectado");
            lblEstado.setForeground(new Color(76, 175, 80));
            
            log("Conectado al servidor: " + servidor + ":" + puerto);
            
            // Iniciar hilo para recibir mensajes
            new HiloRecibir().start();
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al conectar: " + ex.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            log("Error de conexión: " + ex.getMessage());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Puerto inválido", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void desconectar() {
        try {
            conectado = false;
            
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            // Actualizar UI
            btnConectar.setEnabled(true);
            btnDesconectar.setEnabled(false);
            btnEnviar.setEnabled(false);
            txtMensaje.setEnabled(false);
            txtServidor.setEditable(true);
            txtPuerto.setEditable(true);
            
            lblEstado.setText("Desconectado");
            lblEstado.setForeground(Color.RED);
            
            log("Desconectado del servidor");
            
        } catch (IOException ex) {
            log("Error al desconectar: " + ex.getMessage());
        }
    }

    private void enviarMensaje() {
        String mensaje = txtMensaje.getText().trim();
        
        if (!mensaje.isEmpty() && conectado) {
            try {
                salida.println(mensaje);
                log("Tú: " + mensaje);
                txtMensaje.setText("");
                txtMensaje.requestFocus();
            } catch (Exception ex) {
                log("Error al enviar: " + ex.getMessage());
                desconectar();
            }
        }
    }

    private void log(String mensaje) {
        String tiempo = java.time.LocalTime.now().toString().substring(0, 8);
        areaChat.append("[" + tiempo + "] " + mensaje + "\n");
        areaChat.setCaretPosition(areaChat.getDocument().getLength());
    }

    class HiloRecibir extends Thread {
        @Override
        public void run() {
            try {
                String mensaje;
                while (conectado && (mensaje = entrada.readLine()) != null) {
                    log("Servidor: " + mensaje);
                }
            } catch (IOException e) {
                if (conectado) {
                    log("Conexión perdida: " + e.getMessage());
                }
            } finally {
                if (conectado) {
                    desconectar();
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClienteFrame cliente = new ClienteFrame();
            cliente.setVisible(true);
        });
    }
}