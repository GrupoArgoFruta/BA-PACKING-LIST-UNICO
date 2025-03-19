package br.com.argo.controller.packinglist.freshsolution;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.List;

import com.sankhya.util.JdbcUtils;

import br.com.argo.model.ModelClientFreshSolutions;
import br.com.argo.repository.Repository_ClientFreshSolutions;
import br.com.argo.repository.Repository_Invoices;
import br.com.argo.repository.Repository_Packing_Invoice_ClienteFreshSOL;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class Principal implements AcaoRotinaJava {

	@Override
	public void doAction(ContextoAcao ctx) throws Exception {
		// TODO Auto-generated method stub
		 // Obtém as linhas do contexto de ação
	    Registro[] linha = ctx.getLinhas();
		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
		SessionHandle hnd = JapeSession.open();
		NativeSql nativeSql = new NativeSql(jdbc);
		Repository_ClientFreshSolutions  RelatorioClienteFreshSol =  new Repository_ClientFreshSolutions();
		ModelClientFreshSolutions  FreshSolutions =  new ModelClientFreshSolutions();
		Repository_Packing_Invoice_ClienteFreshSOL invoicepackinlist = new Repository_Packing_Invoice_ClienteFreshSOL();
		Repository_Invoices invoices = new Repository_Invoices();
		boolean clienteEncontrado = false;
		StringBuilder mensagemRetorno = new StringBuilder();
		try {
	        for (Registro registro : linha) {
	            BigDecimal codparc = (BigDecimal) registro.getCampo("CODPARC");
	            BigDecimal codTipoOper = (BigDecimal) registro.getCampo("CODTIPOPER");
	            BigDecimal nuNota = (BigDecimal) registro.getCampo("NUNOTA");
	            String opcao = (String) ctx.getParam("OPCAO");
	            String emailParceiroMultiplos = (String) registro.getCampo("AD_EMAILCLIENT");
	            String[] emails = emailParceiroMultiplos.trim().split("\\s*,\\s*");
	            String modal = buscaModal(codTipoOper);
	            List<BigDecimal> Parceiros = FreshSolutions.buscarCodparceFreshSolutions(codparc);
	            
	            // Se o parceiro não for da FreshSolutions, ignorar e continuar
	            if (!Parceiros.contains(codparc)) {
	            	ctx.setMensagemRetorno("O parceiro " + codparc + " não possui Packing List personalizado. Por favor, verifique sua solicitação.");
	            }
	            
				if ("S".equals(opcao)) {
//	               
					RelatorioClienteFreshSol.clienteFreshSolutions(ctx, codparc, nuNota, registro);
					mensagemRetorno.append("O email com a Nota: ").append(nuNota)
							.append(" do PACKING LIST  foi enviado com sucesso.\n");
				} else if ("N".equals(opcao)) {

					switch (modal) {
					case "M":
						invoices.gerarRelatorioInvoiceMaritimo(ctx, nuNota, registro, emailParceiroMultiplos);
						mensagemRetorno.append("O email com a Nota: ").append(nuNota)
								.append(" da INVOICE MARÍTIMO foi enviado com sucesso.\n");
						break;
					case "A":
						invoices.gerarRelatorioAereo(ctx, nuNota, registro, emailParceiroMultiplos);
						mensagemRetorno.append("O email com a Nota: ").append(nuNota)
								.append(" da INVOICE AÉREO foi enviado com sucesso.\n");
						break;
					case "R":
						invoices.gerarRelatorioRodoviario(ctx, nuNota, registro, emailParceiroMultiplos);
						mensagemRetorno.append("O email com a Nota: ").append(nuNota)
								.append(" da INVOICE RODOVIÁRIO foi enviado com sucesso.\n");
						break;
					default:
						ctx.mostraErro("Modalidade não reconhecida para Invoice.");
						break;
					}
				} else if ("ALL".equals(opcao)) {
					switch (modal) {
					case "M":
						invoicepackinlist.clienteMA_FreshSolutions(ctx, codparc, nuNota, registro);
						mensagemRetorno.append("O email com a Nota: ").append(nuNota)
								.append(" da INVOICE MARÍTIMO / PACKING LIST foi enviado com sucesso.\n");
						break;
					case "A":
						invoicepackinlist.clienteAE_FreshSolutions(ctx, codparc, nuNota, registro);
						mensagemRetorno.append("O email com a Nota: ").append(nuNota)
								.append(" da INVOICE AÉREO / PACKING LIST foi enviado com sucesso.\n");
						break;
					case "R":
						invoicepackinlist.clienteRO_FreshSolutions(ctx, codparc, nuNota, registro);
						mensagemRetorno.append("O email com a Nota: ").append(nuNota)
								.append(" da INVOICE RODOVIÁRIO / PACKING LIST foi enviado com sucesso.\n");
						break;
					default:
						ctx.mostraErro("Modalidade não reconhecida para Invoice/packing List.");
						break;
					}
				} else {
					ctx.mostraErro("Opção não reconhecida.");
					return; // Para evitar continuar o loop após um erro
				}
			}

			if (mensagemRetorno.length() > 0) {
				ctx.setMensagemRetorno(mensagemRetorno.toString().trim());
			}
	    } catch (Exception e) {
	        e.printStackTrace();
	        ctx.mostraErro("Erro na classe principal: " + e.getMessage());
	    } finally {
	        JapeSession.close(hnd);
	    }
	}
	public String buscaModal(BigDecimal codTipoOper) throws MGEModelException {
	    JdbcWrapper jdbc = null;
	    NativeSql sql = null;
	    ResultSet rset = null;
	    SessionHandle hnd = null;
	    String result = null;
	    try {
	        hnd = JapeSession.open();
	        hnd.setFindersMaxRows(-1);
	        EntityFacade entity = EntityFacadeFactory.getDWFFacade();
	        jdbc = entity.getJdbcWrapper();
	        jdbc.openSession();

	        sql = new NativeSql(jdbc);

	        sql.appendSql("SELECT AD_MODAL FROM TGFTOP WHERE CODTIPOPER = :CODTIPOPER");

	        sql.setNamedParameter("CODTIPOPER", codTipoOper);
	        rset = sql.executeQuery();

	        if (rset.next()) {
	            result = rset.getString("AD_MODAL");
	        }
	    } catch (Exception e) {
	        MGEModelException.throwMe(e);
	    } finally {
	        JdbcUtils.closeResultSet(rset);
	        NativeSql.releaseResources(sql);
	        JdbcWrapper.closeSession(jdbc);
	        JapeSession.close(hnd);
	    }
	    return result;
	}

}
//	 List<BigDecimal> ParceiroFS = FreshSolutions.buscarCodparceFreshSolutions(codparc);
//	 RelatorioClienteFreshSol.clienteFreshSolutions(ctx, codparc, nuNota, registro);
   
//	if ("S".equals(opcao)) {
//    // Executa apenas para Packing
//    RelatorioClienteFreshSol.clienteFreshSolutions(ctx, codparc, nuNota, registro);
//} else if ("N".equals(opcao)) {
//	invoices.gerarRelatorioInvoiceMaritimo(ctx, nuNota, registro, emailParceiroMultiplos);
//	ctx.setMensagemRetorno(
//            "O email com a Nota:  " + nuNota + "  da INVOICE MARÍTIMO foi enviado com sucesso.");
    // Executa apenas para Invoice
//    String modal = buscaModal(codTipoOper);
//    if (modal.equals("M")) {
//    	invoices.gerarRelatorioInvoiceMaritimo(ctx, nuNota, registro, emailParceiroMultiplos);
//        ctx.setMensagemRetorno(
//                "O email com a Nota:  " + nuNota + "  da INVOICE MARÍTIMO foi enviado com sucesso.");
//    } else if (modal.equals("A")) {
//    	invoices.gerarRelatorioAereo(ctx, nuNota, registro, emailParceiroMultiplos);
//        ctx.setMensagemRetorno(
//                "O email com a Nota:  " + nuNota + "  da INVOICE AEREO foi enviado com sucesso.");
//    } else if (modal.equals("R")) {
//    	invoices.gerarRelatorioRodoviario(ctx, nuNota, registro, emailParceiroMultiplos);
//        ctx.setMensagemRetorno(
//                "O email com a Nota:  " + nuNota + "  da INVOICE RODOVIÁRIO foi enviado com sucesso.");
//    } else {
//        ctx.mostraErro("Modalidade não reconhecida para Invoice.");
//    }
//} 

//else if ("ALL".equals(opcao)) {
//    // Executa para ambos Packing e Invoice
//	invoices.gerarRelatorioInvoiceMaritimo(ctx, nuNota, registro, emailParceiroMultiplos);
//    // Adicione a lógica para Invoice aqui, se necessário
//} else {
//    ctx.mostraErro("Opção inválida selecionada.");
//}
//}
