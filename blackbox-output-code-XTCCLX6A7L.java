import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ServidorFrame extends JFrame {
    private ServerSocket serverSocket;
    private Socket cliente;
    private JTextArea areaLog;
    private JButton btnIniciar, btnDetener;
    private JLabel lblEstado, lblPuerto;
    private JTextField txtPuerto;
    private boolean servidorActivo = false;
    private HiloServidor hiloServidor;

    public ServidorFrame() {
        // Configurar ventana
        setTitle("Servidor TCP - Java Swing");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Panel superior - Controles
        JPanel panel_superior = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        lblPuerto = new JLabel("Puerto:");
        txtPuerto = new JTextField("9000", 8);
        
        btnIniciar = new JButton("Iniciar Servidor");
        btnIniciar.setBackground(new Color(76, 175, 80));
        btnIniciar.setForeground(Color.WHITE);
        
        btnDetener = new JButton("Detener");
        btnDetener.setBackground(new Color(244, 67, 54));
        btnDetener.setForeground(Color.WHITE);
        btnDetener.setEnabled(false);
        
        lblEstado = new JLabel("Estado: Inactivo");
        lblEstado.setForeground(Color.RED);
        
        panel_superior.add(lblPuerto);
        panel_superior.add(txtPuerto);
        panel_superior.add(btnIniciar);
        panel_superior.add(btnDetener);
        panel_superior.add(lblEstado);

        // Panel central - Log de eventos
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(areaLog);
        
        add(panel_superior, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        // Evento botón iniciar
        btnIniciar.addActionListener(e -> iniciarServidor());
        
        // Evento botón detener
        btnDetener.addActionListener(e -> detenerServidor());
        
        // Cerrar ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                detenerServidor();
            }
        });
    }

    private void iniciarServidor() {
        try {
            int puerto = Integer.parseInt(txtPuerto.getText());
            
            if (puerto < 1024 || puerto > 65535) {
                JOptionPane.showMessageDialog(this, 
                    "Puerto debe estar entre 1024 y 65535", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            serverSocket = new ServerSocket(puerto);
            servidorActivo = true;
            
            // Actualizar UI
            btnIniciar.setEnabled(false);
            btnDetener.setEnabled(true);
            txtPuerto.setEditable(false);
            lblEstado.setText("Estado: Activo");
            lblEstado.setForeground(new Color(76, 175, 80));
            
            log("Servidor iniciado en puerto: " + puerto);
            
            // Iniciar hilo del servidor
            hiloServidor = new HiloServidor();
            hiloServidor.start();
            
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, 
                "Error al iniciar servidor: " + ex.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, 
                "Puerto inválido", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void detenerServidor() {
        try {
            servidorActivo = false;
            
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // Actualizar UI
            btnIniciar.setEnabled(true);
            btnDetener.setEnabled(false);
            txtPuerto.setEditable(true);
            lblEstado.setText("Estado: Inactivo");
            lblEstado.setForeground(Color.RED);
            
            log("Servidor detenido");
            
        } catch (IOException ex) {
            log("Error al detener: " + ex.getMessage());
        }
    }

    private void log(String mensaje) {
        String tiempo = java.time.LocalTime.now().toString().substring(0, 8);
        areaLog.append("[" + tiempo + "] " + mensaje + "\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    class HiloServidor extends Thread {
        @Override
        public void run() {
            while (servidorActivo) {
                try {
                    log("Esperando conexión...");
                    cliente = serverSocket.accept();
                    
                    String ipCliente = cliente.getInetAddress().getHostAddress();
                    log("Cliente conectado: " + ipCliente);
                    
                    // Manejar cliente en nuevo hilo
                    new HiloCliente(cliente).start();
                    
                } catch (SocketException e) {
                    if (servidorActivo) {
                        log("Error de socket: " + e.getMessage());
                    }
                } catch (IOException e) {
                    if (servidorActivo) {
                        log("Error: " + e.getMessage());
                    }
                }
            }
        }
    }

    class HiloCliente extends Thread {
        private Socket socket;
        
        public HiloCliente(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                PrintWriter salida = new PrintWriter(
                    socket.getOutputStream(), true);
                
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    log("Recibido: " + mensaje);
                    salida.println("Echo: " + mensaje);
                }
                
                socket.close();
                log("Cliente desconectado");
                
            } catch (IOException e) {
                log("Error con cliente: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServidorFrame frame = new ServidorFrame();
            frame.setVisible(true);
        });
    }
}