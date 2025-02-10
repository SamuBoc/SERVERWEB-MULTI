package Server;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public final class ServidorWeb {
    public static void main(String argv[]) throws Exception {
        // Establece el número de puerto
        int puerto = 6789;
        
        // Establece el socket de escucha
        ServerSocket socketServidor = new ServerSocket(puerto);
        System.out.println("Servidor iniciado en el puerto " + puerto);
        
        // Procesa las solicitudes HTTP en un ciclo infinito
        while (true) {
            // Escucha las solicitudes de conexión TCP
            Socket socketConexion = socketServidor.accept();
            
            // Construye un objeto para procesar el mensaje de solicitud HTTP
            SolicitudHttp solicitud = new SolicitudHttp(socketConexion);
            
            // Crea un nuevo hilo para procesar la solicitud
            Thread hilo = new Thread(solicitud);
            
            // Inicia el hilo
            hilo.start();
        }
    }
}

final class SolicitudHttp implements Runnable {
    final static String CRLF = "\r\n";
    private Socket socket;

    // Constructor
    public SolicitudHttp(Socket socket) {
        this.socket = socket;
    }

    // Implementa el método run() de la interface Runnable.
    public void run() {
        try {
            proceseSolicitud();
        } catch (Exception e) {
            System.out.println("Error procesando la solicitud: " + e.getMessage());
        }
    }

    private void proceseSolicitud() throws Exception {
        // Referencia al stream de salida del socket
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        // Referencia y filtros para el stream de entrada del socket
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Lee el mensaje de solicitud HTTP.
        String lineaDeSolicitud = br.readLine();
        System.out.println();
        System.out.println("Solicitud: " + lineaDeSolicitud);

        // Recoge y muestra las líneas de header.
        String lineaDelHeader;
        while ((lineaDelHeader = br.readLine()).length() != 0) {
            System.out.println(lineaDelHeader);
        }

        // Extrae el nombre del archivo de la línea de solicitud.
        StringTokenizer partesLinea = new StringTokenizer(lineaDeSolicitud);
        partesLinea.nextToken();  // "Salta" sobre el método (se supone que es "GET")
        String nombreArchivo = partesLinea.nextToken();

        // Anexa un ".", para indicar que el archivo se encuentra en el directorio actual.
        nombreArchivo = "." + nombreArchivo;

        // Abre el archivo solicitado.
        FileInputStream fis = null;
        boolean existeArchivo = true;
        try {
            fis = new FileInputStream(nombreArchivo);
        } catch (FileNotFoundException e) {
            existeArchivo = false;
        }

        // Construye el mensaje de respuesta.
        String lineaDeEstado = null;
        String lineaDeTipoContenido = null;
        String cuerpoMensaje = null;
        if (existeArchivo) {
            lineaDeEstado = "HTTP/1.0 200 OK" + CRLF;
            lineaDeTipoContenido = "Content-type: " + contentType(nombreArchivo) + CRLF;
        } else {
            lineaDeEstado = "HTTP/1.0 404 Not Found" + CRLF;
            lineaDeTipoContenido = "Content-type: text/html" + CRLF;
            cuerpoMensaje = "<HTML>" +
                            "<HEAD><TITLE>404 Not Found</TITLE></HEAD>" +
                            "<BODY><b>404</b> Not Found</BODY></HTML>";
        }

        // Envía la línea de estado.
        os.writeBytes(lineaDeEstado);
        // Envía la línea de header content-type.
        os.writeBytes(lineaDeTipoContenido);
        // Envía una línea en blanco para indicar el final de los headers
        os.writeBytes(CRLF);

        // Envía el cuerpo del mensaje
        if (existeArchivo) {
            enviarBytes(fis, os);
            fis.close();
        } else {
            os.writeBytes(cuerpoMensaje);
        }

        // Cierra los streams y el socket.
        os.close();
        br.close();
        socket.close();
    }

    // Método auxiliar para enviar el contenido del archivo.
    private static void enviarBytes(FileInputStream fis, OutputStream os) throws Exception {
        // Buffer de 1KB para transferir los bytes.
        byte[] buffer = new byte[1024];
        int bytes = 0;
        // Copia el archivo solicitado hacia el stream de salida.
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }

    // Método para determinar el tipo MIME del archivo solicitado.
    private static String contentType(String nombreArchivo) {
        if (nombreArchivo.endsWith(".htm") || nombreArchivo.endsWith(".html")) {
            return "text/html";
        }
        if (nombreArchivo.endsWith(".gif")) {
            return "image/gif";
        }
        if (nombreArchivo.endsWith(".jpeg") || nombreArchivo.endsWith(".jpg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
