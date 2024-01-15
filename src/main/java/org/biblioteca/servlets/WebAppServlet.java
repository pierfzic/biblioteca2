package org.biblioteca.servlets;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpSession;
import org.biblioteca.model.Libro;
import org.biblioteca.model.Prestito;
import org.biblioteca.model.Utente;
import org.biblioteca.utils.ConfigLoader;
import org.eclipse.jetty.server.RequestLog;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.biblioteca.Main.PATH_WEBAPP_SERVLET;


@javax.servlet.annotation.WebServlet(name = "WebAppServlet", urlPatterns = {PATH_WEBAPP_SERVLET+"/*"}, loadOnStartup = 2)
public class WebAppServlet extends HttpServlet {


    private ConfigLoader properties;
    private List<Prestito> listaPrestiti;
    private Utente currentUser;

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        this.properties= (ConfigLoader) context.getAttribute("properties");
    }

    @Override
    protected void doGet(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws ServletException, IOException {
        String contextPath = request.getContextPath();
        ServletContext context = getServletContext();
        this.listaPrestiti= (List<Prestito>) context.getAttribute("loans" );
        HttpSession session = request.getSession();
        this.currentUser= (Utente) session.getAttribute("currentUser");
        String statusMsg= (String) session.getAttribute("statusMsg");
        String statusLoanMsg= (String) session.getAttribute("statusLoanMsg");
        List<Libro> searchResults= (List<Libro>) session.getAttribute("searchResults");
        List<Libro> books= (List<Libro>) context.getAttribute("books");
        Integer idUser=-1;
        String username="";
        if (this.currentUser!=null) {
            idUser = this.currentUser.getId();
            username = this.currentUser.getUsername();
        }
        String uri = request.getRequestURI();
        StringBuffer sb=new StringBuffer();

        // Ottiene il servletPath
        String servletPath = request.getServletPath();
        Integer disableAuth=Integer.parseInt(properties.getProperty("disable.auth"));


//        if (!disableAuth.equals(1)) {
//            HttpSession session = request.getSession();
//            username = null;
//            if (session != null && session.getAttribute("username") != null) {
//                // L'utente è loggato
//                 username = (String) session.getAttribute("username");
//                // Gestisci la richiesta dell'utente autenticato
//            } else {
//                // L'utente non è loggato, reindirizza al login
//                response.sendRedirect("/biblioteca/web/login.html");
//            }
//        }

        Map<String, String> mapParameters=new HashMap<>(); //mappa delle variabili template da sostituire
        mapParameters.put("##username##",username);
        mapParameters.put("##risultatiricerca##",genTableSearchResult(searchResults));
        mapParameters.put("##listalibri##", genTableSearchResult(books));
        if (statusMsg!=null)
            mapParameters.put("##statusmsg##", statusMsg);
        else
            mapParameters.put("##statusmsg##", "");
        if (currentUser!=null) {
            List<Prestito> prestitiInSospesoUtente = (List<Prestito>) currentUser.getListaPrestitiUtente().stream().filter(prestito -> (prestito.getRestituito() == null)).toList();
            if (prestitiInSospesoUtente != null)
                mapParameters.put("##borrowedbooks##", genTableLoanResult(prestitiInSospesoUtente));
            else
                mapParameters.put("##borrowedbooks##", "");
            mapParameters.put("##statusloanmsg##", (statusLoanMsg != null) ? statusLoanMsg : "");
        }
        String path = uri.substring(contextPath.length());
        String pathCartella=path.substring(request.getServletPath().length());

//        //redirezione
//        if (pathCartella.startsWith("/registration/") ||
//                (pathCartella.startsWith("/login")) ) {
//            if (!((this.currentUser!=null) && (this.currentUser.getIsAdmin())))
//                response.sendRedirect(contextPath+PATH_WEBAPP_SERVLET+"/login.html");
//
//        }

        // Estrae la parte finale dell'URI per ottenere il nome della pagina
        String pagina = path.substring(path.lastIndexOf('/') + 1);
        String estensione = pagina.substring(pagina.lastIndexOf('.') + 1);

        if (estensione.equals("html")) {
            response.setContentType("text/html;charset=UTF-8"); }
        else {
                response.setContentType("text/"+estensione+";charset=UTF-8");
            }

            //String parameter1 = request.getParameter("parameter1");

            // Ottieni il writer della risposta
            PrintWriter out = response.getWriter();

            // Leggi e restituisci il file HTML
            try (BufferedReader reader = new BufferedReader(new FileReader("."+path))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (FileNotFoundException e) {
                out.println("<html><body><h2>File non trovato</h2></body></html>");
            } catch (IOException e) {
                out.println("<html><body><h2>Errore durante la lettura del file</h2></body></html>");
            }

            String templatePage=sb.toString();
            String filteredPage=replaceStrings(templatePage, mapParameters);

        /* cancellazione dati precedenti */
        session.setAttribute("searchResults",null);
        session.setAttribute("statusMsg",null);
        session.setAttribute("statusLoanMsg",null);

            out.println(filteredPage);



    }

    public  String replaceStringsInFile(String filePath, Map<String, String> replacements) {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath));
        ) {

            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
                // Applica tutte le sostituzioni per la linea corrente
                for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                    currentLine = currentLine.replace(replacement.getKey(), replacement.getValue());
                }
                sb.append(currentLine + System.lineSeparator());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public  String replaceStrings(String template, Map<String, String> replacements) {
        String pageHTML=template;
        // Applica tutte le sostituzioni
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            if (replacement.getValue()!=null)
                pageHTML = pageHTML.replace(replacement.getKey(), replacement.getValue());
        }
        return pageHTML;
    }

    private String genTableSearchResult(List<Libro> results)  {
        if (results==null)
            return "";
        if (results.size()==0)
            return "<h3>Nessun risultato</h3>";
        StringBuilder html = new StringBuilder();
        html.append("<h2>Risultati ricerca:</h2>\n");
        //html.append("<style> table, th, td { border: 1px solid black; border-collapse: collapse; } </style>\n");
        html.append("<table>\n");
        html.append("<tr><th>Autore</th><th>Titolo</th><th>Editore</th><th>Anno</th><th>operazione</th></tr>\n");

        if (results==null)
            return "";
        for (Libro libro : results) {
            html.append("<tr>");
            html.append("<td>").append(libro.getAutore()).append("</td>");
            html.append("<td>").append(libro.getTitolo()).append("</td>");
            html.append("<td>").append(libro.getEditore()).append("</td>");
            html.append("<td>").append(libro.getAnno()).append("</td>");
            html.append("<td>").append("<button onclick=\"window.location.href='/biblioteca/backend/chiediprestito?libro=").append(libro.getId()).append("';\">Chiedi Prestito</a>").append("</td>");
            html.append("</tr>\n");
        }

        html.append("</table>");
//        html.append(" <script>\n" +
//                "        function redirectToBook(int n) {\n" +
//                "            window.location.href = \"http://localhost:8080/biblioteca/services/prestito?utente=1&libro=\"+n;\n" +
//                "        };\n" +
//                "    </script>");

        return html.toString();

    }
    private String genTableLoanResult(List<Prestito> results)  {
        if ((results==null) || (results.size()==0) )
            return "<h3>Nessun prestito</h3>";
        StringBuilder html = new StringBuilder();
        //html.append("<style> table, th, td { border: 1px solid black; border-collapse: collapse; } </style>\n");
        html.append("<table>\n");
        html.append("<tr><th>Autore</th><th>Titolo</th><th>Editore</th><th>Anno</th><th>Scadenza</th><th>operazione</th></tr>\n");

        if (results==null)
            return "";
        for (Prestito prestito : results) {
            html.append("<tr>");
            html.append("<td>").append(prestito.getLibro().getAutore()).append("</td>");
            html.append("<td>").append(prestito.getLibro().getTitolo()).append("</td>");
            html.append("<td>").append(prestito.getLibro().getEditore()).append("</td>");
            html.append("<td>").append(prestito.getLibro().getAnno()).append("</td>");
            html.append("<td>").append(prestito.getScadenza().toString()).append("</td>");
            //html.append("<td>").append("<a href='/biblioteca/backend/restituisciprestito?prestito=").append(prestito.getId()).append("'>Restituisci Prestito</a>");
            html.append("<td>").append("<button onclick=\"window.location.href='/biblioteca/backend/restituisciprestito?prestito=").append(prestito.getId()).append("';\">Restituisci Prestito</a>");
            //html.append(" ").append("<a href='/biblioteca/backend/prorogaprestito?prestito=").append(prestito.getId()).append("'>Proroga Prestito</a>");
            html.append(" ").append("<button onclick=\"window.location.href='/biblioteca/backend/prorogaprestito?prestito=").append(prestito.getId()).append("';\">Proroga Prestito</a>");
            //html.append(" ").append("<a href='/biblioteca/backend/notificascadenza?prestito=").append(prestito.getId()).append("'>Notifica Scadenza</a>");
            html.append(" ").append("<button onclick=\"window.location.href='/biblioteca/backend/notificascadenza?prestito=").append(prestito.getId()).append("';\">Notifica Scadenza</a>");
            html.append("</td>");
            html.append("</tr>\n");
        }

        html.append("</table>");
//        html.append(" <script>\n" +
//                "        function redirectToBook(int n) {\n" +
//                "            window.location.href = \"http://localhost:8080/biblioteca/services/prestito?utente=1&libro=\"+n;\n" +
//                "        };\n" +
//                "    </script>");

        return html.toString();

    }

    private String getScript(String msg) {
        StringBuilder sb=new StringBuilder();
        sb.append("<script type='text/javascript'>");
        // Controlla la condizione qui e genera lo script di conseguenza
        sb.append("document.addEventListener('DOMContentLoaded', (event) => {\n")
        //sb.append("window.onload = function() {\n")
                .append("  alert('"+msg+"');\n")
                .append("};\n");

        sb.append("</script>");
        return sb.toString();
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}