package br.com.argo.service;

import java.math.BigDecimal;
import java.sql.ResultSet;

import com.sankhya.util.BigDecimalUtil;
import com.sankhya.util.TimeUtils;

import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;

public class ServiceEmails {
	String msg;
	public	void enviarEmailComAnexos(EntityFacade dwfEntityFacade, ContextoAcao contexto, byte[] bytesPackinglist, byte[] byteMaritmo, String assunto, String mensagem, String emailParam, String emailcliente, String emailGrupo) throws Exception {
		BigDecimal codFila = null;
        BigDecimal nuAnexoPackingList = null;
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
            dynamicVO1.setProperty("EMAIL", emailcliente);
            dynamicVO1.setProperty("CODSMTP", null);
            dynamicVO1.setProperty("CODUSUREMET", contexto.getUsuarioLogado());
            dynamicVO1.setProperty("MIMETYPE", "text/html");
            PersistentLocalEntity createEntity = dwfEntityFacade.createEntity("MSDFilaMensagem", (EntityVO) dynamicVO1);
            codFila = ((DynamicVO) createEntity.getValueObject()).asBigDecimal("CODFILA");

            // Passo 2: Criação dos Anexos na AnexoMensagem para obter os NUANEXO
            // Anexo packing list 
            DynamicVO dynamicVO2PackingList = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoMensagem");
            dynamicVO2PackingList.setProperty("ANEXO", bytesPackinglist);
            dynamicVO2PackingList.setProperty("NOMEARQUIVO","PACKING_LIST.xls"); // Nome do arquivo Excel
            dynamicVO2PackingList.setProperty("TIPO","application/vnd.ms-excel"); // Tipo MIME para Excel
            createEntity = dwfEntityFacade.createEntity("AnexoMensagem", (EntityVO) dynamicVO2PackingList);
            nuAnexoPackingList = ((DynamicVO) createEntity.getValueObject()).asBigDecimal("NUANEXO");
            
        	DynamicVO dynamicVO2Invoice = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoMensagem");
			dynamicVO2Invoice.setProperty("ANEXO", byteMaritmo);
			dynamicVO2Invoice.setProperty("NOMEARQUIVO", "INVOICE.pdf");
			dynamicVO2Invoice.setProperty("TIPO", "application/pdf");
			createEntity = dwfEntityFacade.createEntity("AnexoMensagem", (EntityVO) dynamicVO2Invoice);
			nuAnexoInvoice = ((DynamicVO) createEntity.getValueObject()).asBigDecimal("NUANEXO");

            // Passo 3: Associação dos Anexos à Mensagem na TMDAXM
            // Anexo packing list 
            DynamicVO dynamicVO3PackingList = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoPorMensagem");
            dynamicVO3PackingList.setProperty("CODFILA", codFila);
            dynamicVO3PackingList.setProperty("NUANEXO", nuAnexoPackingList);
            dwfEntityFacade.createEntity("AnexoPorMensagem", (EntityVO) dynamicVO3PackingList);
            
            // Anexo invoice 
            DynamicVO dynamicVO3Invoice = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("AnexoPorMensagem");
			dynamicVO3Invoice.setProperty("CODFILA", codFila);
			dynamicVO3Invoice.setProperty("NUANEXO", nuAnexoInvoice);
			dwfEntityFacade.createEntity("AnexoPorMensagem", (EntityVO) dynamicVO3Invoice);
			String querySeq  = ("SELECT MAX(SEQUENCIA) + 1 AS SEQUENCIA FROM TMDFMD WHERE CODFILA = " + codFila);
			ResultSet rsSeq = nativeSql.executeQuery(querySeq);
			
			while (rsSeq.next()) {
				seq = rsSeq.getBigDecimal("SEQUENCIA");
			}
			//Email grupo 
			DynamicVO dynamicVO4EmailCopia = (DynamicVO) dwfEntityFacade.getDefaultValueObjectInstance("MSDDestFilaMensagem");
			dynamicVO4EmailCopia.setProperty("CODFILA", codFila);
			dynamicVO4EmailCopia.setProperty("EMAIL", emailGrupo.trim());
			dynamicVO4EmailCopia.setProperty("SEQUENCIA", seq);
			dwfEntityFacade.createEntity("MSDDestFilaMensagem", (EntityVO) dynamicVO4EmailCopia);
			
			} catch (Exception e) {
				e.printStackTrace();
				msg = "Erro na criação da mensagem: " + e.getMessage();
				contexto.setMensagemRetorno(msg);
			}finally { 
				JdbcWrapper.closeSession(jdbc); 
				NativeSql.releaseResources(nativeSql);
			}
		}
	
	
	public	void enviarEmailInvoiceComAnexos(EntityFacade dwfEntityFacade, ContextoAcao contexto, byte[] bytesPdfInvoice,
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
