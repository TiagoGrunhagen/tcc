/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ufpr.tads.tcc.servlets;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;
import com.ufpr.tads.tcc.beans.Evento;
import com.ufpr.tads.tcc.beans.Usuario;
import com.ufpr.tads.tcc.facade.EventoFacade;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.sql.Date;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 *
 * @author mateus
 */
@WebServlet(name = "EventoServlet", urlPatterns = {"/EventoServlet"})
@MultipartConfig
public class EventoServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        String acao = request.getParameter("action");
        
        Usuario lb = (Usuario) session.getAttribute("usuario");
        if (lb == null) {
            request.setAttribute("msg", "Usuário deve se autenticar para acessar o sistema.");

            RequestDispatcher rd = getServletContext().getRequestDispatcher("/index.jsp");
            rd.forward(request, response);
            return;
        }
        
        if (acao == null || acao.equals("list")) {
            List<Evento> eventos;
            try {
                eventos = EventoFacade.buscarTodosEventosPorIdUsuario(lb.getId());
                request.setAttribute("eventos", eventos);
            } catch (SQLException | ClassNotFoundException ex) {
                request.setAttribute("exception", ex);
                RequestDispatcher rd = getServletContext().getRequestDispatcher("/erro.jsp");
                rd.forward(request, response);
            }
            RequestDispatcher rd = getServletContext().getRequestDispatcher("/eventosListar.jsp");
            rd.forward(request, response);
        } else {
            if (acao.equals("show")) {
                try {
                    int id = Integer.parseInt(request.getParameter("id"));
                    Evento evento = EventoFacade.buscar(id);
                    request.setAttribute("visualizarevento", evento);
                } catch (NumberFormatException | SQLException | ClassNotFoundException ex) {
                    request.setAttribute("exception", ex);
                    request.setAttribute("javax.servlet.error.status_code", 500);
                    RequestDispatcher rd = getServletContext().getRequestDispatcher("/erro.jsp");
                    rd.forward(request, response);
                }
                RequestDispatcher rd = getServletContext().getRequestDispatcher("/eventosVisualizar.jsp");
                rd.forward(request, response);
            } else { 
                if (acao.equals("formUpdate")) {
                    try {
                        int id = Integer.parseInt(request.getParameter("id"));
                        Evento evento = EventoFacade.buscar(id);
                        request.setAttribute("alterarevento", evento);
                    } catch (NumberFormatException | SQLException | ClassNotFoundException ex) {
                        request.setAttribute("exception", ex);
                        request.setAttribute("javax.servlet.error.status_code", 500);
                        RequestDispatcher rd = getServletContext().getRequestDispatcher("/erro.jsp");
                        rd.forward(request, response);
                    }
                    
                    RequestDispatcher rd = getServletContext().getRequestDispatcher("/eventosForm.jsp?form=alterar");
                    rd.forward(request, response);
                } else {
                    if (acao.equals("remove")) {
                        try {
                            int id = Integer.parseInt(request.getParameter("id"));
                            EventoFacade.remover(id);
                        } catch (NumberFormatException | SQLException | ClassNotFoundException ex) {
                            request.setAttribute("exception", ex);
                            request.setAttribute("javax.servlet.error.status_code", 500);
                            RequestDispatcher rd = getServletContext().getRequestDispatcher("/erro.jsp");
                            rd.forward(request, response);
                        }
                        RequestDispatcher rd = getServletContext().getRequestDispatcher("/EventoServlet?action=list");
                        rd.forward(request, response);
                    } else {
                        if (acao.equals("update")) {
                            Evento evento = new Evento();
                            try {
                                int id = Integer.parseInt(request.getParameter("id"));
                                evento.setId(id);
                                String currentPath = "";
                                final Part filePart = request.getPart("img");
                                final String fileName = getFileName(filePart);
                                if (filePart.getSize() > 0 ) {
                                    String path = request.getServletContext().getRealPath("img")+ File.separator;
                                    String totalPath = path + fileName;
                                    currentPath = "/tcc/img/" + fileName;
                                    OutputStream out = null;
                                    InputStream filecontent = null;
                                    final PrintWriter writer = response.getWriter();

                                    try {
                                        out = new FileOutputStream(new File(totalPath));
                                        filecontent = filePart.getInputStream();

                                        int read = 0;
                                        final byte[] bytes = new byte[1024];

                                        while ((read = filecontent.read(bytes)) != -1) {
                                            out.write(bytes, 0, read);
                                        }
                                    } catch (FileNotFoundException ex) {
                                        request.setAttribute("exception", ex);
                                        request.setAttribute("javax.servlet.error.status_code", 500);
                                        RequestDispatcher rd = getServletContext().getRequestDispatcher("/erro.jsp");
                                        rd.forward(request, response);
                                    }
                                }
                                
                                evento.setImagem(currentPath);
                                evento.setNome(request.getParameter("nome"));
                                evento.setDescrição(request.getParameter("desc"));
                                evento.setEndereco(request.getParameter("endereco"));

                                String dataInicioString = request.getParameter("dataInicio");
                                String dataFimString = request.getParameter("dataFim");
                                DateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                                Date data = new Date(fmt.parse(dataInicioString).getTime());
                                evento.setDataInicio(data);
                                data = new Date(fmt.parse(dataFimString).getTime());
                                evento.setDataFim(data);
                                if (evento.getImagem() != null && evento.getImagem() != "") {
                                    EventoFacade.alterar(evento);
                                } else {
                                    EventoFacade.alterarSemImagem(evento);
                                }
                                
                                RequestDispatcher rd = getServletContext().getRequestDispatcher("/EventoServlet?action=list");
                                rd.forward(request, response);
                            } catch (NumberFormatException | ParseException | SQLException | ClassNotFoundException ex) {
                                request.setAttribute("exception", ex);
                                request.setAttribute("javax.servlet.error.status_code", 500);
                                RequestDispatcher rd = getServletContext().getRequestDispatcher("/erro.jsp");
                                rd.forward(request, response);
                            }
                        } else {
                            if (acao.equals("formNew")) {
                                RequestDispatcher rd = getServletContext().getRequestDispatcher("/eventosForm.jsp");
                                rd.forward(request, response);
                            } else {
                                if (acao.equals("new")) {
                                    Evento evento = new Evento();
                                    String currentPath = "";
                                    final Part filePart = request.getPart("img");
                                    final String fileName = getFileName(filePart);
                                    if (filePart.getSize() > 0 ) {
                                        String path = request.getServletContext().getRealPath("img")+ File.separator;
                                        String totalPath = path + fileName;
                                        currentPath = "/tcc/img/" + fileName;
                                        OutputStream out = null;
                                        InputStream filecontent = null;
                                        final PrintWriter writer = response.getWriter();

                                        try {
                                            out = new FileOutputStream(new File(totalPath));
                                            filecontent = filePart.getInputStream();

                                            int read = 0;
                                            final byte[] bytes = new byte[1024];

                                            while ((read = filecontent.read(bytes)) != -1) {
                                                out.write(bytes, 0, read);
                                            }
                                        } catch (FileNotFoundException ex) {
                                            request.setAttribute("exception", ex);
                                            request.setAttribute("javax.servlet.error.status_code", 500);
                                            RequestDispatcher rd = getServletContext().getRequestDispatcher("/erro.jsp");
                                            rd.forward(request, response);
                                        }
                                    }
                                    evento.setImagem(currentPath);
                                    evento.setNome(request.getParameter("nome"));
                                    evento.setDescrição(request.getParameter("desc"));
                                    evento.setEndereco(request.getParameter("endereco"));
                                    evento.setAprovado(false);
                                    evento.setUsuario(lb);

                                    String dataInicioString = request.getParameter("dataInicio");
                                    String dataFimString = request.getParameter("dataFim");
                                    DateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                                    try {
                                        Date data = new Date(fmt.parse(dataInicioString).getTime());
                                        evento.setDataInicio(data);
                                        data = new Date(fmt.parse(dataFimString).getTime());
                                        evento.setDataFim(data);
                                    } catch (ParseException ex) {
                                        request.setAttribute("exception", ex);
                                        RequestDispatcher rd = getServletContext().getRequestDispatcher("/erro.jsp");
                                        rd.forward(request, response);
                                    }

                                    try {
                                        EventoFacade.inserir(evento);
                                    } catch (SQLException | ClassNotFoundException  ex) {
                                        request.setAttribute("exception", ex);
                                        request.setAttribute("javax.servlet.error.status_code", 500);
                                        RequestDispatcher rd = getServletContext().getRequestDispatcher("/erro.jsp");
                                        rd.forward(request, response);
                                    }
                                    RequestDispatcher rd = getServletContext().getRequestDispatcher("/EventoServlet?action=list");
                                    rd.forward(request, response);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    private String getFileName(final Part part) {
    final String partHeader = part.getHeader("content-disposition");
    LOGGER.log(Level.INFO, "Part Header = {0}", partHeader);
    for (String content : part.getHeader("content-disposition").split(";")) {
        if (content.trim().startsWith("filename")) {
            return content.substring(
                    content.indexOf('=') + 1).trim().replace("\"", "");
        }
    }
    return null;
}

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
