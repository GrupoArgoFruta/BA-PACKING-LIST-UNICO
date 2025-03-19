package br.com.argo.repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.SessionFile;
import com.sankhya.util.TimeUtils;

import br.com.argo.service.ServiceEmails;
import br.com.argo.service.ServiceEmailsInvoices;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.AgendamentoRelatorioHelper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.AgendamentoRelatorioHelper.ParametroRelatorio;
import br.com.sankhya.ws.ServiceContext;

public class Repository_Invoices {
	ServiceEmailsInvoices emails = new ServiceEmailsInvoices ();
	String msg;
//	ServiceEmails  emailsAnexos = new  ServiceEmails ();
	public void gerarRelatorioInvoiceMaritimo(ContextoAcao ctx, BigDecimal nuanexo, Registro registro, String emailParceiroMultiplos) throws Exception {
		BigDecimal nuRfe = new BigDecimal(250);
		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
		SessionHandle hnd = JapeSession.open();
		NativeSql nativeSql = new NativeSql(jdbc);
		ResultSet rset = null;
		List<Object> lstParam = new ArrayList<Object>();
		byte[] pdfBytesInvoice = null;
//	    Registro[] linha = ctx.getLinhas();
		String Container = (String) registro.getCampo("AD_EX_CONTAINER");
		String Descrnavio = (String) registro.getCampo("VESSEL");
		Date Eta = (Date) registro.getCampo("ETA");
		String nomeporto = (String) registro.getCampo("PORTO_DESTINO");
		String indentificacao = (String) registro.getCampo("IDENTIFICACAO");
		String obs = (String) ctx.getParam("OBSERVACAO");
		String emailAdc = (String) ctx.getParam("EMAILADC");
		int codUsu = ctx.getUsuarioLogado().intValue();
		String email = null;
//comercial.uva@argofruta.com
		try {
			jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
            ResultSet query = nativeSql.executeQuery("SELECT AD_GRUPOMAIL FROM TSIUSU WHERE CODUSU = " + codUsu);
            while (query.next()) {
                email = query.getString("AD_GRUPOMAIL");
            }

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			ParametroRelatorio pk = new ParametroRelatorio("PK_NUNOTA", BigDecimal.class.getName(), nuanexo);
			lstParam.add(pk);

			// Gere o relatório principal (Invoice Marítimo)
			pdfBytesInvoice = AgendamentoRelatorioHelper.getPrintableReport(nuRfe, lstParam, ctx.getUsuarioLogado(),
					dwfFacade);

			// Crie os SessionFile para os anexos
			SessionFile sessionFileInvoice = SessionFile.createSessionFile("INVOICE_MARÍTIMO.pdf", "INVOICE_MARÍTIMO",
					pdfBytesInvoice);

			// Adicione os SessionFile na sessão
			ServiceContext.getCurrent().putHttpSessionAttribute("invoice", sessionFileInvoice);
			// Construindo o assunto do email
			String assunto = "";
		
			assunto += (indentificacao != null ? indentificacao : "");
			assunto += (Descrnavio != null ? " Ship name: " + Descrnavio : "");
			assunto += (Container != null ? " Container: " + Container : "");
//			String saudacaoIngles = obterSaudacaoIngles();
//			String saudacaoEspanol = obterSaudacaoEspanol();
			// Construindo o corpo do email em HTML com tabelas
			String mensagem = "<!DOCTYPE html>" + 
			    "<html>" + 
			    "<head>" + 
			    "    <meta charset=\"utf-8\"/>" +
			    "    <title>Email</title>" + 
			    "    <style>" + 
			    "        table {" +
			    "            border-collapse: collapse;" + 
			    "            width: 100%;" + 
			    "        }" +
			    "        th, td {" + 
			    "            border: 1px solid black;" + 
			    "            padding: 8px;" +
			    "            text-align: left;" + 
			    "        }" + 
			    "    </style>" + 
			    "</head>" + 
			    "<body>";

			mensagem += "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"star1\">" + 
			    "        <tr>" +
			    "            <td align=\"center\" style=\"background-color:#1e6533;\">" +
			    "                <div class=\"image-container\">" +
			    "                    <img border=\"0\" style=\"width:17%;\"" +
			    "                        src=\"https://argofruta.com/wp-content/uploads/2021/05/Logo-text-green.png\" alt=\"\">" +
			    "                </div>" + 
			    "            </td>" + 
			    "        </tr>" + 
			    "    </table>" +
			    "    <h2>Container Details</h2>" + 
			    "    <table>";

			// Inserindo informações do Container
			mensagem += "        <tr>" + 
			    "            <td>Container</td>" + 
			    "            <td>" + (Container != null ? Container : "") + "</td>" + 
			    "        </tr>" + 
			    "        <tr>" +
			    "            <td>Vessel</td>" + 
			    "            <td>" + (Descrnavio != null ? Descrnavio : "") + "</td>" + 
			    "        </tr>";

			// Formatando a data de ETA se não for nula
			if (Eta != null) {
			    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			    String etaFormatted = dateFormat.format(Eta);
			    mensagem += "        <tr>" + 
			        "            <td>ETA</td>" + 
			        "            <td>" + etaFormatted + "</td>" +
			        "        </tr>";
			}

			// Adicionando Port of Destination
			mensagem += "        <tr>" + 
			    "            <td>Port of Destination</td>" + 
			    "            <td>" + (nomeporto != null ? nomeporto : "") + "</td>" + 
			    "        </tr>";

			// Adicionando Argo Reference
			mensagem += "        <tr>" + 
			    "            <td>Argo Reference</td>" + 
			    "            <td>" + (indentificacao != null ? indentificacao : "") + "</td>" + 
			    "        </tr>";

			// Adicionando Observação
			if (obs != null && !obs.isEmpty()) {
			    mensagem += "        <tr>" + 
			        "            <td>Observation</td>" + 
			        "            <td>" + obs + "</td>" +
			        "        </tr>";
			}

			mensagem += "    </table>";



			mensagem += "</body></html>";

			// Envie o e-mail com os dois anexos
			enviarEmailComAnexos(dwfFacade, ctx, pdfBytesInvoice, assunto, mensagem,emailParceiroMultiplos,email,emailAdc);

		} catch (Exception e) {
			e.printStackTrace();
			RuntimeException re = new RuntimeException(e);
			System.out.println("Erro ao gerar invoice marítimo: " + e.getCause() + e.getMessage());
			throw re;
		} finally {
			JapeSession.close(hnd);
			JdbcWrapper.closeSession(jdbc);
			NativeSql.releaseResources(nativeSql);
		}
	}
	public void gerarRelatorioAereo(ContextoAcao ctx, BigDecimal nuanexo,Registro registro, String emailParceiro) throws Exception {
	    BigDecimal nuRfe = new BigDecimal(99);
		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
		SessionHandle hnd = JapeSession.open();
		NativeSql nativeSql = new NativeSql(jdbc);
		ResultSet rset = null;
	    List<Object> lstParam = new ArrayList<Object>();
	    byte[] pdfBytesInvoice = null;
//	    Registro[] linha = ctx.getLinhas();
	    String Container = (String) registro.getCampo("AD_EX_CONTAINER");
	    String Descrnavio = (String) registro.getCampo("VESSEL");
		Date Eta = (Date) registro.getCampo("ETA");
		String nomeporto = (String) registro.getCampo("NOMPORTO");
		String indentificacao = (String) registro.getCampo("IDENTIFICACAO");
		String obs = (String) ctx.getParam("OBSERVACAO");
		 String emailAdc = (String) ctx.getParam("EMAILADC");
	    int codUsu = ctx.getUsuarioLogado().intValue();
	    String email = null;
	    try {
	    	
	    	jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
            ResultSet query = nativeSql.executeQuery("SELECT AD_GRUPOMAIL FROM TSIUSU WHERE CODUSU = " + codUsu);
            while (query.next()) {
                email = query.getString("AD_GRUPOMAIL");
            }
	    	EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			ParametroRelatorio pk = new ParametroRelatorio("PK_NUNOTA", BigDecimal.class.getName(), nuanexo);
			lstParam.add(pk);

			// Gere o relatório principal (Invoice Aereo)
			pdfBytesInvoice = AgendamentoRelatorioHelper.getPrintableReport(nuRfe, lstParam, ctx.getUsuarioLogado(),dwfFacade);
		
			// Crie os SessionFile para os anexos
			SessionFile sessionFileInvoice = SessionFile.createSessionFile("INVOICE_AEREO.pdf", "INVOICE_MARÍTIMO",pdfBytesInvoice);
			
			// Adicione os SessionFile na sessão
			ServiceContext.getCurrent().putHttpSessionAttribute("invoice", sessionFileInvoice);
			  // Construindo o assunto do email
			String assunto = "";
			assunto += (indentificacao != null ? indentificacao : "");
			assunto += (Descrnavio != null ? " Ship name: " + Descrnavio : "");
			assunto += (Container != null ? " Container: " + Container : "");

//			String saudacaoIngles = obterSaudacaoIngles();
//			String saudacaoEspanol = obterSaudacaoEspanol();
			// Construindo o corpo do email em HTML com tabelas
			String mensagem = "<!DOCTYPE html>" + 
			    "<html>" + 
			    "<head>" + 
			    "    <meta charset=\"utf-8\"/>" +
			    "    <title>Email</title>" + 
			    "    <style>" + 
			    "        table {" +
			    "            border-collapse: collapse;" + 
			    "            width: 100%;" + 
			    "        }" +
			    "        th, td {" + 
			    "            border: 1px solid black;" + 
			    "            padding: 8px;" +
			    "            text-align: left;" + 
			    "        }" + 
			    "    </style>" + 
			    "</head>" + 
			    "<body>";

			mensagem += "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"star1\">" + 
			    "        <tr>" +
			    "            <td align=\"center\" style=\"background-color:#1e6533;\">" +
			    "                <div class=\"image-container\">" +
			    "                    <img border=\"0\" style=\"width:17%;\"" +
			    "                        src=\"https://argofruta.com/wp-content/uploads/2021/05/Logo-text-green.png\" alt=\"\">" +
			    "                </div>" + 
			    "            </td>" + 
			    "        </tr>" + 
			    "    </table>" +
			    "    <h2>Container Details</h2>" + 
			    "    <table>";

			// Inserindo informações do Container
			mensagem += "        <tr>" + 
			    "            <td>Container</td>" + 
			    "            <td>" + (Container != null ? Container : "") + "</td>" + 
			    "        </tr>" + 
			    "        <tr>" +
			    "            <td>Vessel</td>" + 
			    "            <td>" + (Descrnavio != null ? Descrnavio : "") + "</td>" + 
			    "        </tr>";

			// Formatando a data de ETA se não for nula
			if (Eta != null) {
			    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			    String etaFormatted = dateFormat.format(Eta);
			    mensagem += "        <tr>" + 
			        "            <td>ETA</td>" + 
			        "            <td>" + etaFormatted + "</td>" +
			        "        </tr>";
			}

			// Adicionando Port of Destination
			mensagem += "        <tr>" + 
			    "            <td>Port of Destination</td>" + 
			    "            <td>" + (nomeporto != null ? nomeporto : "") + "</td>" + 
			    "        </tr>";

			// Adicionando Argo Reference
			mensagem += "        <tr>" + 
			    "            <td>Argo Reference</td>" + 
			    "            <td>" + (indentificacao != null ? indentificacao : "") + "</td>" + 
			    "        </tr>";

			// Adicionando Observação
			if (obs != null && !obs.isEmpty()) {
			    mensagem += "        <tr>" + 
			        "            <td>Observation</td>" + 
			        "            <td>" + obs + "</td>" +
			        "        </tr>";
			}

			mensagem += "    </table>";

		

			mensagem += "</body></html>";

						// Envie o e-mail com os dois anexos
			enviarEmailComAnexos(dwfFacade, ctx, pdfBytesInvoice, assunto, mensagem,emailParceiro,email,emailAdc);
	    } catch (Exception e) {
	        e.printStackTrace();
	        RuntimeException re = new RuntimeException(e);
	        System.out.println("Erro ao gerar invoice Aéreo: " + e.getCause() + e.getMessage());
	        throw re;
	    }finally { 
			JapeSession.close(hnd); 
			JdbcWrapper.closeSession(jdbc); 
			NativeSql.releaseResources(nativeSql);
		}
	}
	public void gerarRelatorioRodoviario(ContextoAcao ctx, BigDecimal nuanexo,Registro registro, String emailParceiro) throws Exception {
	    BigDecimal nuRfe = new BigDecimal(92);
		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
		SessionHandle hnd = JapeSession.open();
		NativeSql nativeSql = new NativeSql(jdbc);
		ResultSet rset = null;
	    List<Object> lstParam = new ArrayList<Object>();
	    byte[] pdfBytesInvoice = null;
//	    Registro[] linha = ctx.getLinhas();
	    String Container = (String) registro.getCampo("AD_EX_CONTAINER");
	    String Descrnavio = (String) registro.getCampo("VESSEL");
		Date Eta = (Date) registro.getCampo("ETA");
		String nomeporto = (String)registro.getCampo("NOMPORTO");
		String indentificacao = (String) registro.getCampo("IDENTIFICACAO");
		String obs = (String) ctx.getParam("OBSERVACAO");
		 String emailAdc = (String) ctx.getParam("EMAILADC");
	    int codUsu = ctx.getUsuarioLogado().intValue();
	    String emailGrupo = null;
	    try {
	    	   jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
               ResultSet query = nativeSql.executeQuery("SELECT AD_GRUPOMAIL FROM TSIUSU WHERE CODUSU = " + codUsu);
               while (query.next()) {
            	   emailGrupo = query.getString("AD_GRUPOMAIL");
               }
	    	EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			ParametroRelatorio pk = new ParametroRelatorio("PK_NUNOTA", BigDecimal.class.getName(), nuanexo);
			lstParam.add(pk);
			// Gere o relatório principal (Invoice Aereo)
			pdfBytesInvoice = AgendamentoRelatorioHelper.getPrintableReport(nuRfe, lstParam, ctx.getUsuarioLogado(),dwfFacade);
			
			// Crie os SessionFile para os anexos
			SessionFile sessionFileInvoice = SessionFile.createSessionFile("INVOICE_RODOVIÁRIO.pdf", "INVOICE_RODOVIÁRIO",pdfBytesInvoice);
			  // Construindo o assunto do email
			// Adicione os SessionFile na sessão
			ServiceContext.getCurrent().putHttpSessionAttribute("invoice", sessionFileInvoice);
			String assunto = "";
			assunto += (indentificacao != null ? indentificacao : "");
			assunto += (Descrnavio != null ? " Ship name: " + Descrnavio : "");
			assunto += (Container != null ? " Container: " + Container : "");

//			String saudacaoIngles = obterSaudacaoIngles();
//			String saudacaoEspanol = obterSaudacaoEspanol();
			// Construindo o corpo do email em HTML com tabelas
			String mensagem = "<!DOCTYPE html>" + 
			    "<html>" + 
			    "<head>" + 
			    "    <meta charset=\"utf-8\"/>" +
			    "    <title>Email</title>" + 
			    "    <style>" + 
			    "        table {" +
			    "            border-collapse: collapse;" + 
			    "            width: 100%;" + 
			    "        }" +
			    "        th, td {" + 
			    "            border: 1px solid black;" + 
			    "            padding: 8px;" +
			    "            text-align: left;" + 
			    "        }" + 
			    "    </style>" + 
			    "</head>" + 
			    "<body>";

			mensagem += "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" class=\"star1\">" + 
			    "        <tr>" +
			    "            <td align=\"center\" style=\"background-color:#1e6533;\">" +
			    "                <div class=\"image-container\">" +
			    "                    <img border=\"0\" style=\"width:17%;\"" +
			    "                        src=\"https://argofruta.com/wp-content/uploads/2021/05/Logo-text-green.png\" alt=\"\">" +
			    "                </div>" + 
			    "            </td>" + 
			    "        </tr>" + 
			    "    </table>" +
			    "    <h2>Container Details</h2>" + 
			    "    <table>";

			// Inserindo informações do Container
			mensagem += "        <tr>" + 
			    "            <td>Container</td>" + 
			    "            <td>" + (Container != null ? Container : "") + "</td>" + 
			    "        </tr>" + 
			    "        <tr>" +
			    "            <td>Vessel</td>" + 
			    "            <td>" + (Descrnavio != null ? Descrnavio : "") + "</td>" + 
			    "        </tr>";

			// Formatando a data de ETA se não for nula
			if (Eta != null) {
			    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
			    String etaFormatted = dateFormat.format(Eta);
			    mensagem += "        <tr>" + 
			        "            <td>ETA</td>" + 
			        "            <td>" + etaFormatted + "</td>" +
			        "        </tr>";
			}

			// Adicionando Port of Destination
			mensagem += "        <tr>" + 
			    "            <td>Port of Destination</td>" + 
			    "            <td>" + (nomeporto != null ? nomeporto : "") + "</td>" + 
			    "        </tr>";

			// Adicionando Argo Reference
			mensagem += "        <tr>" + 
			    "            <td>Argo Reference</td>" + 
			    "            <td>" + (indentificacao != null ? indentificacao : "") + "</td>" + 
			    "        </tr>";

			// Adicionando Observação
			if (obs != null && !obs.isEmpty()) {
			    mensagem += "        <tr>" + 
			        "            <td>Observation</td>" + 
			        "            <td>" + obs + "</td>" +
			        "        </tr>";
			}

			mensagem += "    </table>";


			mensagem += "</body></html>";
			// Envie o e-mail com os dois anexos
		enviarEmailComAnexos(dwfFacade, ctx, pdfBytesInvoice, assunto, mensagem,emailParceiro,emailGrupo,emailAdc);
	    } catch (Exception e) {
	        e.printStackTrace();
	        RuntimeException re = new RuntimeException(e);
	        System.out.println("Erro ao gerar gerarRelatorioRodoviario: " + e.getCause() + e.getMessage());
	        throw re;
	    }finally { 
			JapeSession.close(hnd); 
			JdbcWrapper.closeSession(jdbc); 
			NativeSql.releaseResources(nativeSql);
		}
	}
	private void enviarEmailComAnexos(EntityFacade dwfEntityFacade, ContextoAcao contexto, byte[] bytesPdfInvoice,
		 String assunto, String mensagem,String emailclient, String emailGrupo, String emailAdc) throws Exception {
		BigDecimal codFila = null;
		BigDecimal nuAnexoInvoice = null;
		BigDecimal seq = null;
		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();

		NativeSql nativeSql = new NativeSql(jdbc);

		try {
			// Passo 1: Criação da Mensagem na MSDFilaMensagem para obter o CODFILA
			DynamicVO dynamicVO1 = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("MSDFilaMensagem");
			dynamicVO1.setProperty("ASSUNTO", assunto);
			dynamicVO1.setProperty("CODMSG", null);
			dynamicVO1.setProperty("DTENTRADA", TimeUtils.getNow());
			dynamicVO1.setProperty("STATUS", "Pendente");
			dynamicVO1.setProperty("CODCON", BigDecimal.ZERO);
			dynamicVO1.setProperty("TENTENVIO", BigDecimalUtil.valueOf(3));
			dynamicVO1.setProperty("MENSAGEM", mensagem.toCharArray());
			dynamicVO1.setProperty("TIPOENVIO", "E");
			dynamicVO1.setProperty("MAXTENTENVIO", BigDecimalUtil.valueOf(3));
			dynamicVO1.setProperty("EMAIL", emailclient.trim());
			dynamicVO1.setProperty("CODSMTP", null);
			dynamicVO1.setProperty("CODUSUREMET",contexto.getUsuarioLogado());
			dynamicVO1.setProperty("MIMETYPE", "text/html");
			PersistentLocalEntity createEntity = dwfEntityFacade.createEntity("MSDFilaMensagem", (EntityVO) dynamicVO1);
			codFila = ((DynamicVO) createEntity.getValueObject()).asBigDecimal("CODFILA");

			// Passo 2: Criação dos Anexos na AnexoMensagem para obter os NUANEXO
			// Anexo Invoice
			DynamicVO dynamicVO2Invoice = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoMensagem");
			dynamicVO2Invoice.setProperty("ANEXO", bytesPdfInvoice);
			dynamicVO2Invoice.setProperty("NOMEARQUIVO", "INVOICE.pdf");
			dynamicVO2Invoice.setProperty("TIPO", "application/pdf");
			createEntity = dwfEntityFacade.createEntity("AnexoMensagem", (EntityVO) dynamicVO2Invoice);
			nuAnexoInvoice = ((DynamicVO) createEntity.getValueObject()).asBigDecimal("NUANEXO");

			// Passo 3: Associação dos Anexos à Mensagem na TMDAXM
			// Anexo Invoice
			DynamicVO dynamicVO3Invoice = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoPorMensagem");
			dynamicVO3Invoice.setProperty("CODFILA", codFila);
			dynamicVO3Invoice.setProperty("NUANEXO", nuAnexoInvoice);
			dwfEntityFacade.createEntity("AnexoPorMensagem", (EntityVO) dynamicVO3Invoice);
			
			String querySeq  = ("SELECT MAX(SEQUENCIA) + 1 AS SEQUENCIA FROM TMDFMD WHERE CODFILA = " + codFila);
			ResultSet rsSeq = nativeSql.executeQuery(querySeq);
			
			while (rsSeq.next()) {
				seq = rsSeq.getBigDecimal("SEQUENCIA");
			}
			//Passo 4 : Email copia  na tabela que gerar uma copia de email 
			DynamicVO dynamicVO4EmailCopia = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("MSDDestFilaMensagem");
			dynamicVO4EmailCopia.setProperty("CODFILA", codFila);
			dynamicVO4EmailCopia.setProperty("EMAIL", emailGrupo.trim());
			dynamicVO4EmailCopia.setProperty("SEQUENCIA", seq);
			dwfEntityFacade.createEntity("MSDDestFilaMensagem", (EntityVO) dynamicVO4EmailCopia);
			//Email adcional  
			DynamicVO dynamicVO5EmailAdc = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("MSDDestFilaMensagem");
			dynamicVO5EmailAdc.setProperty("CODFILA", codFila);
			dynamicVO5EmailAdc.setProperty("EMAIL", emailAdc.trim());
			dynamicVO5EmailAdc.setProperty("SEQUENCIA", seq);
			dwfEntityFacade.createEntity("MSDDestFilaMensagem", (EntityVO) dynamicVO5EmailAdc);
			contexto.setMensagemRetorno("E-mail com anexos enviado com sucesso.");
			
			contexto.setMensagemRetorno("O e-mail do usuário logado é: " + (emailGrupo != null ? emailGrupo : "Nenhum e-mail encontrado."));
		} catch (Exception e) {
			e.printStackTrace();
			msg = "Erro na criação da mensagem: " + e.getMessage();
			contexto.setMensagemRetorno(msg);
		}finally { 
			JdbcWrapper.closeSession(jdbc); 
			NativeSql.releaseResources(nativeSql);
		}
	}

}
