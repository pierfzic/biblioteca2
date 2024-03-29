package org.biblioteca.servlets;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpSession;
import org.biblioteca.Main;
import org.biblioteca.model.Libro;
import org.biblioteca.model.Prestito;
import org.biblioteca.model.Utente;
import org.biblioteca.utils.ConfigLoader;
import org.biblioteca.utils.CustomLogger;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.biblioteca.Main.PATH_BACKEND_SERVLET;
import static org.biblioteca.Main.PATH_WEBAPP_SERVLET;


@javax.servlet.annotation.WebServlet(name = "WebAppServlet", urlPatterns = {PATH_WEBAPP_SERVLET+"/*"}, loadOnStartup = 3)
public class WebAppServlet extends HttpServlet {


    private ConfigLoader properties;
    private Utente currentUser;

    private final CustomLogger logger = CustomLogger.getInstance();

    @Override
    public void init() throws ServletException {
        ServletContext context = getServletContext();
        this.properties= (ConfigLoader) context.getAttribute("properties");
    }

    @Override
    protected void doGet(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) throws ServletException, IOException {
        String contextPath = request.getContextPath();
        ServletContext context = getServletContext();
        HttpSession session = request.getSession();
        this.currentUser= (Utente) session.getAttribute("currentUser");
        String statusMsg= (String) session.getAttribute("statusMsg");
        String statusLoanMsg= (String) session.getAttribute("statusLoanMsg");
        String hideloginmenu= (String) session.getAttribute("hideloginmenu");
        List<Libro> searchResults= (List<Libro>) session.getAttribute("searchResults");
        List<Libro> books= (List<Libro>) context.getAttribute("books");
        List<Utente> users= (List<Utente>) context.getAttribute("users");
        String username="";
        if (this.currentUser!=null) {
            username = this.currentUser.getUsername();
        }
        String uri = request.getRequestURI();
        String path = uri.substring(contextPath.length()+1);
        // Estrae la parte finale dell'URI per ottenere il nome della pagina
        String pagina = path.substring(path.lastIndexOf('/') + 1);
        String estensione = pagina.substring(pagina.lastIndexOf('.') + 1);
        StringBuffer sb=new StringBuffer();
        // Ottieni il writer della risposta
        PrintWriter out = response.getWriter();

                //check autenticazione
            username = null;
        //boolean firstLogin = session.getAttribute("firstLogin") != null;
        boolean firstLogin = estensione.equals("css");
        if ((session != null && session.getAttribute("username") != null) ||firstLogin ) {
                // L'utente è loggato
                 username = (String) session.getAttribute("username");
                 session.removeAttribute("firstLogin");
                // Gestisci la richiesta dell'utente autenticato
            } else {
                // L'utente non è loggato, reindirizza al login
                response.sendRedirect("/biblioteca/web/login.html");
            }
        Map<String, String> mapParameters= null; //mappa delle variabili template da sostituire
        if (estensione.equals("html")) {
            // per evitare che tornando indietro risulti loggato
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0
            response.setDateHeader("Expires", 0); // Proxies.
            // -------------------
            if (currentUser!=null) {
            mapParameters = new HashMap<>();
            mapParameters.put("##username##",username);
            mapParameters.put("##risultatiricerca##",genTableSearchResult(searchResults, contextPath, PATH_BACKEND_SERVLET));
            mapParameters.put("##listalibri##", genTableListResult(books,contextPath, PATH_BACKEND_SERVLET));
            mapParameters.put("##statusmsg##", (statusMsg!=null)? statusMsg: "" );
            mapParameters.put("##hideloginmenu##", (hideloginmenu!=null)? hideloginmenu: "" );
            mapParameters.put("##listautenti##", genTableUserResult(users,contextPath, PATH_BACKEND_SERVLET));

            List<Prestito> prestitiInSospesoUtente = currentUser.getListaPrestitiUtente().stream().filter(prestito -> (prestito.getRestituito() == null)).toList();
            mapParameters.put("##borrowedbooks##", (prestitiInSospesoUtente != null)?genTableLoanResult(prestitiInSospesoUtente,contextPath, PATH_BACKEND_SERVLET):"");
            mapParameters.put("##statusloanmsg##", (statusLoanMsg != null) ? statusLoanMsg : "");
            mapParameters.put("##onlyforadmin##", this.currentUser.getIsAdmin()? " style=\"display: block;\"": "style=\"display: none;\"");
            }
        }

        response.setContentType("text/"+estensione+";charset=UTF-8");
            // Leggi e restituisci il file HTML

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(path);
        System.out.println("Sto servendo il file "+path);
        logger.logInfo("WebAppServlet: sto servendo il file "+path);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
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
        //session.setAttribute("hideloginmenu", null);
        out.println(filteredPage);
}
    public  String replaceStrings(String template, Map<String, String> replacements) {
        if (replacements==null)
            return template;
        String pageHTML=template;
        // Applica tutte le sostituzioni
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            if (replacement.getValue()!=null)
                pageHTML = pageHTML.replace(replacement.getKey(), replacement.getValue());
        }
        return pageHTML;
    }

    private String genTableSearchResult(List<Libro> results, String contextPath, String servletContextPath)  {
        if (results==null)
            return "";
        if (results.size()==0)
            return "<h3>Nessun risultato</h3>";
        StringBuilder html = new StringBuilder();
        html.append("<h2>Risultati ricerca:</h2>\n");
        html.append("<table class=\"table-search\">\n");
        html.append("<tr><th>Autore</th><th>Titolo</th><th>Editore</th><th>Anno</th><th>Disponibilità</th><th>Operazione</th></tr>\n");

        if (results==null)
            return "";
        for (Libro libro : results) {
            html.append("<tr>");
            html.append("<td>").append(libro.getAutore()).append("</td>");
            html.append("<td>").append(libro.getTitolo()).append("</td>");
            html.append("<td>").append(libro.getEditore()).append("</td>");
            html.append("<td>").append(libro.getAnno()).append("</td>");
            html.append("<td>").append(libro.getDisponibilita()).append("</td>");
            html.append("<td>").append("<div class='operation'>").append("<button onclick=\"window.location.href='")
                    .append(contextPath).append(servletContextPath).append("/chiediprestito?libro=").append(libro.getId())
                    .append("';\">Chiedi Prestito</a>").append("</div>").append("</td>");
            html.append("</tr>\n");
        }
        html.append("</table>");
        return html.toString();

    }

    private String genTableUserResult(List<Utente> results, String contextPath, String servletContextPath) throws IOException {
        if (results==null)
            return "";
        if (results.size()==0)
            return "<h3>Nessun Utente</h3>";
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"table-users\">\n");
        html.append("<tr><th>Id</th><th>Username</th><th>Amministratore</th><th>N. Libri in prestito</th><th>Operazione</th></tr>\n");

        if (results==null)
            return "";
        for (Utente utente : results) {
            html.append("<tr>");
            html.append("<td>").append(utente.getId()).append("</td>");
            html.append("<td>").append(utente.getUsername()).append("</td>");

            html.append("<td>").append("<div class=\"check-admin\">" )
                    .append("<label class=\"container\">")
                    .append(getCheckboxAdmin(utente.getIsAdmin(),utente.getId())).append("</div>").append("</td>");
            html.append("<td>").append(utente.getListaPrestitiUtente().size()).append("</td>");
            html.append("<td>").append("<div class='operation'>").append("<button class='deletebutton' onclick=\"window.location.href='")
                    .append(contextPath).append(servletContextPath).append("/eliminaUtente?utente=").append(utente.getId())
                    .append("'\">Elimina</button>").append("</td>");
            html.append("</tr>\n");
        }
        html.append("</table>");
        return html.toString();

    }

    private String genTableListResult(List<Libro> results, String contextPath, String servletContextPath)  {
        if (results==null)
            return "";
        if (results.size()==0)
            return "<h3>Nessun libro in biblioteca</h3>";
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"table-books\">\n");
        html.append("<tr><th>Autore</th><th>Titolo</th><th>Editore</th><th>Anno</th><th>Disponibilità</th><th>Operazione</th></tr>\n");

        if (results==null)
            return "";
        for (Libro libro : results) {
            html.append("<tr>");
            html.append("<td>").append(libro.getAutore()).append("</td>");
            html.append("<td>").append(libro.getTitolo()).append("</td>");
            html.append("<td>").append(libro.getEditore()).append("</td>");
            html.append("<td>").append(libro.getAnno()).append("</td>");
            html.append("<td>").append(libro.getDisponibilita()).append("</td>");
            html.append("<td>");
            html.append("<div class='operation'>");
            if (libro.getDisponibilita()>0)
                       html.append("<button onclick=\"window.location.href='")
                    .append(contextPath).append(servletContextPath).append("/chiediprestito?libro=").append(libro.getId())
                    .append("';\">Chiedi Prestito</a>");
            if (currentUser.getIsAdmin()) {
                html.append("<button class='deletebutton' onclick=\"window.location.href='")
                        .append(contextPath).append(servletContextPath).append("/cancellalibro?libro=").append(libro.getId())
                        .append("';\">Cancella Libro</a>");
            }
            html.append("</div>");
            html.append("</td>");
            html.append("</tr>\n");
        }
        html.append("</table>");
        return html.toString();

    }
    private String genTableLoanResult(List<Prestito> results, String contextPath, String servletContextPath)  {
        if ((results==null) || (results.size()==0) )
            return "<h3>Nessun prestito</h3>";
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"table-loans\">\n");
        html.append("<tr><th>Autore</th><th>Titolo</th><th>Editore</th><th>Anno</th><th>Scadenza</th><th>Operazione</th></tr>\n");

        if (results==null)
            return "";
        for (Prestito prestito : results) {
            html.append("<tr>");
            html.append("<td>").append(prestito.getLibro().getAutore()).append("</td>");
            html.append("<td>").append(prestito.getLibro().getTitolo()).append("</td>");
            html.append("<td>").append(prestito.getLibro().getEditore()).append("</td>");
            html.append("<td>").append(prestito.getLibro().getAnno()).append("</td>");
            html.append("<td>").append(prestito.getScadenza().toString()).append("</td>");
            html.append("<td>").append("<div class='operation'>").append("<button onclick=\"window.location.href='")
                    .append(contextPath).append(servletContextPath).append("/restituisciprestito?prestito=").append(prestito.getId())
                    .append("';\">Restituisci Prestito</a>");
            html.append(" ").append("<button onclick=\"window.location.href='").append(contextPath).append(servletContextPath)
                    .append("/prorogaprestito?prestito=").append(prestito.getId()).append("';\">Proroga Prestito</a>");
            html.append(" ").append("<button onclick=\"window.location.href='").append(contextPath).append(servletContextPath)
                    .append("/notificascadenza?prestito=").append(prestito.getId()).append("';\">Notifica Scadenza</a>");
            html.append("</div>").append("</td>");
            html.append("</tr>\n");
        }
        html.append("</table>");
        return html.toString();

    }

    private String getCheckboxAdmin(boolean value, int id) {
        StringBuilder sb=new StringBuilder();
        String checkValue=value?"checked": "";
        sb.append("<input type=\"checkbox\" id=\"isAdmin\" name=\"isAdmin\" value=\"admin\" ")
                .append(checkValue).append(" ")
                .append("data-url=\"").append(id).append("\"").append(id).append("\" ")
                .append(" onchange= ")
                .append("\"reindirizza(this)\"")
                .append(" >")
                .append("<span class=\"checkmark\"></span></label>");
        return sb.toString();
    }
    @Override
    public void destroy() {
        super.destroy();
    }
}