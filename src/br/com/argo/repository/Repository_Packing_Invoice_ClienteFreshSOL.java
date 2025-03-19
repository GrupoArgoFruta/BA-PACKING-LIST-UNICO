package br.com.argo.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sankhya.util.SessionFile;


import br.com.argo.service.ServiceEmails;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.AgendamentoRelatorioHelper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.AgendamentoRelatorioHelper.ParametroRelatorio;
import br.com.sankhya.ws.ServiceContext;

public class Repository_Packing_Invoice_ClienteFreshSOL {
	
	
	public void clienteMA_FreshSolutions(ContextoAcao ctx, BigDecimal codparc, BigDecimal nuanexo, Registro registro) {
	    BigDecimal nuRfe = new BigDecimal(261);
	    BigDecimal nuRfemaritino = new BigDecimal(250); // relatorio maritmo
	    ServiceEmails emailsAnexos = new ServiceEmails();
//	    ModelosPackingList principalPackingList = new ModelosPackingList();
	    JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
	    SessionHandle hnd = JapeSession.open();
	    NativeSql nativeSql = new NativeSql(jdbc);
	    ResultSet rset = null;
	    List<Object> lstParam = new ArrayList<Object>();
	    byte[] excelBytesPackinglist = null;
	    byte[] byteMaritmo = null;
	    int codUsu = ctx.getUsuarioLogado().intValue();
		String Container = (String) registro.getCampo("AD_EX_CONTAINER");
		String Descrnavio = (String) registro.getCampo("VESSEL");
		Date Eta = (Date) registro.getCampo("ETA");
		String nomeporto = (String) registro.getCampo("PORTO_DESTINO");
		String indentificacao = (String) registro.getCampo("IDENTIFICACAO");
		String obs = (String) ctx.getParam("OBSERVACAO");
		String emailParam = (String) ctx.getParam("EMAILADC");
	    String emailGrupo = null;
	    String emailcliente = (String) registro.getCampo("AD_EMAILCLIENT");
	    String[] emails = emailcliente.trim().split("\\s*,\\s*");

	    try {
	        
	        jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
	        ResultSet query = nativeSql.executeQuery("SELECT AD_GRUPOMAIL FROM TSIUSU WHERE CODUSU = " + codUsu);
	        while (query.next()) {
	        	emailGrupo = query.getString("AD_GRUPOMAIL");
	        }

	        EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
	        ParametroRelatorio pk = new ParametroRelatorio("PK_NUNOTA", BigDecimal.class.getName(), nuanexo);
	        lstParam.add(pk);

	        // Gere o relatório principal (packing list westfalia)
	        excelBytesPackinglist = AgendamentoRelatorioHelper.getPrintableReport(nuRfe, lstParam, ctx.getUsuarioLogado(), dwfFacade);
	        // Gere o relatório principal (invoice maritimo)
	        byteMaritmo = AgendamentoRelatorioHelper.getPrintableReport(nuRfemaritino, lstParam,ctx.getUsuarioLogado(), dwfFacade);
	        
	        // Crie os SessionFile para os anexos
	        SessionFile sessionFilePcklist = SessionFile.createSessionFile("PACKING_LIST_OGL.xls", "PACKING_LIST_OGL", excelBytesPackinglist);
	        SessionFile sessionFileInvoice = SessionFile.createSessionFile("INVOICE_MARÍTIMO.pdf", "INVOICE_MARÍTIMO", byteMaritmo);
	        // Adicione os SessionFile na sessão
	        ServiceContext.getCurrent().putHttpSessionAttribute("packinglist", sessionFilePcklist);
	        ServiceContext.getCurrent().putHttpSessionAttribute("invoice", sessionFileInvoice);

	     // Construindo o assunto do email
	        String assunto = "";
	        assunto += (indentificacao != null ? indentificacao : "");
			assunto += (Descrnavio != null ? " Ship name: " + Descrnavio : "");
			assunto += (Container != null ? " Container: " + Container : "");
			// Construindo o corpo do email em HTML com tabelas
						String mensagem = "<!DOCTYPE html>" + "<html>" + "<head>" + "    <meta charset=\"utf-8\"/>"
								+ "    <title>Email</title>" + "    <style>" + "        table {"
								+ "            border-collapse: collapse;" + "            width: 100%;" + "        }"
								+ "        th, td {" + "            border: 1px solid black;" + "            padding: 8px;"
								+ "            text-align: left;" + "        }" + "    </style>" + "</head>" + "<body>";

						mensagem += "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"star1\">" + "        <tr>"
								+ "            <td align=\"center\" style=\"background-color:#1e6533;\">"
								+ "                <div class=\"image-container\">"
								+ "                    <img border=\"0\" style=\"width:17%;\""
								+ "                        src=\"https://argofruta.com/wp-content/uploads/2021/05/Logo-text-green.png\" alt=\"\">"
								+ "                </div>" + "            </td>" + "        </tr>" + "    </table>"
								+ "    <h2>Container Details</h2>" + "    <table>";

						// Inserindo informações do Container
						mensagem += "        <tr>" + "            <td>Container</td>" + "            <td>"
								+ (Container != null ? Container : "") + "</td>" + "        </tr>" + "        <tr>"
								+ "            <td>Vessel</td>" + "            <td>" + (Descrnavio != null ? Descrnavio : "")
								+ "</td>" + "        </tr>";

						// Formatando a data de ETA se não for nula
						if (Eta != null) {
							SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
							String etaFormatted = dateFormat.format(Eta);
							mensagem += "        <tr>" + "            <td>ETA</td>" + "            <td>" + etaFormatted + "</td>"
									+ "        </tr>";
						}

						// Adicionando Port of Destination
						mensagem += "        <tr>" + "            <td>Port of Destination</td>" + "            <td>"
								+ (nomeporto != null ? nomeporto : "") + "</td>" + "        </tr>";

						// Adicionando Argo Reference
						mensagem += "        <tr>" + "            <td>Argo Reference</td>" + "            <td>"
								+ (indentificacao != null ? indentificacao : "") + "</td>" + "        </tr>";

						// Adicionando Observação
						if (obs != null && !obs.isEmpty()) {
							mensagem += "        <tr>" + "            <td>Observation</td>" + "            <td>" + obs + "</td>"
									+ "        </tr>";
						}

						mensagem += "    </table>" + "</body>" + "</html>";
	        

	        // Chama o método enviarEmailComAnexos da classe EnvioEmails
	        emailsAnexos.enviarEmailComAnexos(dwfFacade, ctx, excelBytesPackinglist,byteMaritmo,assunto, mensagem,emailParam,emailcliente,emailGrupo);

	    } catch (Exception e) {
	        // Handle exception
	        e.printStackTrace();
	        ctx.setMensagemRetorno("Erro ao executar clienteWestfalia: " + e.getMessage());
	    } finally {
	        JapeSession.close(hnd);
	        JdbcWrapper.closeSession(jdbc);
	        NativeSql.releaseResources(nativeSql);
	    }
	}
	public void clienteAE_FreshSolutions(ContextoAcao ctx, BigDecimal codparc, BigDecimal nuanexo, Registro registro) {
	    BigDecimal nuRfe = new BigDecimal(261);
	    BigDecimal nuRfemaritino = new BigDecimal(99); // relatorio maritmo
	    ServiceEmails emailsAnexos = new ServiceEmails();
//	    ModelosPackingList principalPackingList = new ModelosPackingList();
	    JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
	    SessionHandle hnd = JapeSession.open();
	    NativeSql nativeSql = new NativeSql(jdbc);
	    ResultSet rset = null;
	    List<Object> lstParam = new ArrayList<Object>();
	    byte[] excelBytesPackinglist = null;
	    byte[] byteMaritmo = null;
	    int codUsu = ctx.getUsuarioLogado().intValue();
		String Container = (String) registro.getCampo("AD_EX_CONTAINER");
		String Descrnavio = (String) registro.getCampo("VESSEL");
		Date Eta = (Date) registro.getCampo("ETA");
		String nomeporto = (String) registro.getCampo("PORTO_DESTINO");
		String indentificacao = (String) registro.getCampo("IDENTIFICACAO");
		String obs = (String) ctx.getParam("OBSERVACAO");
		String emailParam = (String) ctx.getParam("EMAILADC");
	    String emailGrupo = null;
	    String emailcliente = (String) registro.getCampo("AD_EMAILCLIENT");
	    String[] emails = emailcliente.trim().split("\\s*,\\s*");

	    try {
	        
	        jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
	        ResultSet query = nativeSql.executeQuery("SELECT AD_GRUPOMAIL FROM TSIUSU WHERE CODUSU = " + codUsu);
	        while (query.next()) {
	        	emailGrupo = query.getString("AD_GRUPOMAIL");
	        }

	        EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
	        ParametroRelatorio pk = new ParametroRelatorio("PK_NUNOTA", BigDecimal.class.getName(), nuanexo);
	        lstParam.add(pk);

	        // Gere o relatório principal (packing list westfalia)
	        excelBytesPackinglist = AgendamentoRelatorioHelper.getPrintableReport(nuRfe, lstParam, ctx.getUsuarioLogado(), dwfFacade);
	        // Gere o relatório principal (invoice maritimo)
	        byteMaritmo = AgendamentoRelatorioHelper.getPrintableReport(nuRfemaritino, lstParam,ctx.getUsuarioLogado(), dwfFacade);
	        
	        // Crie os SessionFile para os anexos
	        SessionFile sessionFilePcklist = SessionFile.createSessionFile("PACKING_LIST_OGL.xls", "PACKING_LIST_OGL", excelBytesPackinglist);
	        SessionFile sessionFileInvoice = SessionFile.createSessionFile("INVOICE_MARÍTIMO.pdf", "INVOICE_MARÍTIMO", byteMaritmo);
	        // Adicione os SessionFile na sessão
	        ServiceContext.getCurrent().putHttpSessionAttribute("packinglist", sessionFilePcklist);
	        ServiceContext.getCurrent().putHttpSessionAttribute("invoice", sessionFileInvoice);

	     // Construindo o assunto do email
	        String assunto = "";
	        assunto += (indentificacao != null ? indentificacao : "");
			assunto += (Descrnavio != null ? " Ship name: " + Descrnavio : "");
			assunto += (Container != null ? " Container: " + Container : "");
			// Construindo o corpo do email em HTML com tabelas
						String mensagem = "<!DOCTYPE html>" + "<html>" + "<head>" + "    <meta charset=\"utf-8\"/>"
								+ "    <title>Email</title>" + "    <style>" + "        table {"
								+ "            border-collapse: collapse;" + "            width: 100%;" + "        }"
								+ "        th, td {" + "            border: 1px solid black;" + "            padding: 8px;"
								+ "            text-align: left;" + "        }" + "    </style>" + "</head>" + "<body>";

						mensagem += "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"star1\">" + "        <tr>"
								+ "            <td align=\"center\" style=\"background-color:#1e6533;\">"
								+ "                <div class=\"image-container\">"
								+ "                    <img border=\"0\" style=\"width:17%;\""
								+ "                        src=\"https://argofruta.com/wp-content/uploads/2021/05/Logo-text-green.png\" alt=\"\">"
								+ "                </div>" + "            </td>" + "        </tr>" + "    </table>"
								+ "    <h2>Container Details</h2>" + "    <table>";

						// Inserindo informações do Container
						mensagem += "        <tr>" + "            <td>Container</td>" + "            <td>"
								+ (Container != null ? Container : "") + "</td>" + "        </tr>" + "        <tr>"
								+ "            <td>Vessel</td>" + "            <td>" + (Descrnavio != null ? Descrnavio : "")
								+ "</td>" + "        </tr>";

						// Formatando a data de ETA se não for nula
						if (Eta != null) {
							SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
							String etaFormatted = dateFormat.format(Eta);
							mensagem += "        <tr>" + "            <td>ETA</td>" + "            <td>" + etaFormatted + "</td>"
									+ "        </tr>";
						}

						// Adicionando Port of Destination
						mensagem += "        <tr>" + "            <td>Port of Destination</td>" + "            <td>"
								+ (nomeporto != null ? nomeporto : "") + "</td>" + "        </tr>";

						// Adicionando Argo Reference
						mensagem += "        <tr>" + "            <td>Argo Reference</td>" + "            <td>"
								+ (indentificacao != null ? indentificacao : "") + "</td>" + "        </tr>";

						// Adicionando Observação
						if (obs != null && !obs.isEmpty()) {
							mensagem += "        <tr>" + "            <td>Observation</td>" + "            <td>" + obs + "</td>"
									+ "        </tr>";
						}

						mensagem += "    </table>" + "</body>" + "</html>";
	        

	        // Chama o método enviarEmailComAnexos da classe EnvioEmails
	        emailsAnexos.enviarEmailComAnexos(dwfFacade, ctx, excelBytesPackinglist,byteMaritmo,assunto, mensagem,emailParam,emailcliente,emailGrupo);

	    } catch (Exception e) {
	        // Handle exception
	        e.printStackTrace();
	        ctx.setMensagemRetorno("Erro ao executar clienteWestfalia: " + e.getMessage());
	    } finally {
	        JapeSession.close(hnd);
	        JdbcWrapper.closeSession(jdbc);
	        NativeSql.releaseResources(nativeSql);
	    }
	}
	public void clienteRO_FreshSolutions(ContextoAcao ctx, BigDecimal codparc, BigDecimal nuanexo, Registro registro) {
	    BigDecimal nuRfe = new BigDecimal(261);
	    BigDecimal nuRfemaritino = new BigDecimal(92); // relatorio maritmo
	    ServiceEmails emailsAnexos = new ServiceEmails();
//	    ModelosPackingList principalPackingList = new ModelosPackingList();
	    JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
	    SessionHandle hnd = JapeSession.open();
	    NativeSql nativeSql = new NativeSql(jdbc);
	    ResultSet rset = null;
	    List<Object> lstParam = new ArrayList<Object>();
	    byte[] excelBytesPackinglist = null;
	    byte[] byteMaritmo = null;
	    int codUsu = ctx.getUsuarioLogado().intValue();
		String Container = (String) registro.getCampo("AD_EX_CONTAINER");
		String Descrnavio = (String) registro.getCampo("VESSEL");
		Date Eta = (Date) registro.getCampo("ETA");
		String nomeporto = (String) registro.getCampo("PORTO_DESTINO");
		String indentificacao = (String) registro.getCampo("IDENTIFICACAO");
		String obs = (String) ctx.getParam("OBSERVACAO");
		String emailParam = (String) ctx.getParam("EMAILADC");
	    String emailGrupo = null;
	    String emailcliente = (String) registro.getCampo("AD_EMAILCLIENT");
	    String[] emails = emailcliente.trim().split("\\s*,\\s*");

	    try {
	        
	        jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
	        ResultSet query = nativeSql.executeQuery("SELECT AD_GRUPOMAIL FROM TSIUSU WHERE CODUSU = " + codUsu);
	        while (query.next()) {
	        	emailGrupo = query.getString("AD_GRUPOMAIL");
	        }

	        EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
	        ParametroRelatorio pk = new ParametroRelatorio("PK_NUNOTA", BigDecimal.class.getName(), nuanexo);
	        lstParam.add(pk);

	        // Gere o relatório principal (packing list westfalia)
	        excelBytesPackinglist = AgendamentoRelatorioHelper.getPrintableReport(nuRfe, lstParam, ctx.getUsuarioLogado(), dwfFacade);
	        // Gere o relatório principal (invoice maritimo)
	        byteMaritmo = AgendamentoRelatorioHelper.getPrintableReport(nuRfemaritino, lstParam,ctx.getUsuarioLogado(), dwfFacade);
	        
	        // Crie os SessionFile para os anexos
	        SessionFile sessionFilePcklist = SessionFile.createSessionFile("PACKING_LIST_OGL.xls", "PACKING_LIST_OGL", excelBytesPackinglist);
	        SessionFile sessionFileInvoice = SessionFile.createSessionFile("INVOICE_MARÍTIMO.pdf", "INVOICE_MARÍTIMO", byteMaritmo);
	        // Adicione os SessionFile na sessão
	        ServiceContext.getCurrent().putHttpSessionAttribute("packinglist", sessionFilePcklist);
	        ServiceContext.getCurrent().putHttpSessionAttribute("invoice", sessionFileInvoice);

	     // Construindo o assunto do email
	        String assunto = "";
	        assunto += (indentificacao != null ? indentificacao : "");
			assunto += (Descrnavio != null ? " Ship name: " + Descrnavio : "");
			assunto += (Container != null ? " Container: " + Container : "");
			// Construindo o corpo do email em HTML com tabelas
						String mensagem = "<!DOCTYPE html>" + "<html>" + "<head>" + "    <meta charset=\"utf-8\"/>"
								+ "    <title>Email</title>" + "    <style>" + "        table {"
								+ "            border-collapse: collapse;" + "            width: 100%;" + "        }"
								+ "        th, td {" + "            border: 1px solid black;" + "            padding: 8px;"
								+ "            text-align: left;" + "        }" + "    </style>" + "</head>" + "<body>";

						mensagem += "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"star1\">" + "        <tr>"
								+ "            <td align=\"center\" style=\"background-color:#1e6533;\">"
								+ "                <div class=\"image-container\">"
								+ "                    <img border=\"0\" style=\"width:17%;\""
								+ "                        src=\"https://argofruta.com/wp-content/uploads/2021/05/Logo-text-green.png\" alt=\"\">"
								+ "                </div>" + "            </td>" + "        </tr>" + "    </table>"
								+ "    <h2>Container Details</h2>" + "    <table>";

						// Inserindo informações do Container
						mensagem += "        <tr>" + "            <td>Container</td>" + "            <td>"
								+ (Container != null ? Container : "") + "</td>" + "        </tr>" + "        <tr>"
								+ "            <td>Vessel</td>" + "            <td>" + (Descrnavio != null ? Descrnavio : "")
								+ "</td>" + "        </tr>";

						// Formatando a data de ETA se não for nula
						if (Eta != null) {
							SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
							String etaFormatted = dateFormat.format(Eta);
							mensagem += "        <tr>" + "            <td>ETA</td>" + "            <td>" + etaFormatted + "</td>"
									+ "        </tr>";
						}

						// Adicionando Port of Destination
						mensagem += "        <tr>" + "            <td>Port of Destination</td>" + "            <td>"
								+ (nomeporto != null ? nomeporto : "") + "</td>" + "        </tr>";

						// Adicionando Argo Reference
						mensagem += "        <tr>" + "            <td>Argo Reference</td>" + "            <td>"
								+ (indentificacao != null ? indentificacao : "") + "</td>" + "        </tr>";

						// Adicionando Observação
						if (obs != null && !obs.isEmpty()) {
							mensagem += "        <tr>" + "            <td>Observation</td>" + "            <td>" + obs + "</td>"
									+ "        </tr>";
						}

						mensagem += "    </table>" + "</body>" + "</html>";
	        

	        // Chama o método enviarEmailComAnexos da classe EnvioEmails
	        emailsAnexos.enviarEmailComAnexos(dwfFacade, ctx, excelBytesPackinglist,byteMaritmo,assunto, mensagem,emailParam,emailcliente,emailGrupo);

	    } catch (Exception e) {
	        // Handle exception
	        e.printStackTrace();
	        ctx.setMensagemRetorno("Erro ao executar clienteWestfalia: " + e.getMessage());
	    } finally {
	        JapeSession.close(hnd);
	        JdbcWrapper.closeSession(jdbc);
	        NativeSql.releaseResources(nativeSql);
	    }
	}
}
