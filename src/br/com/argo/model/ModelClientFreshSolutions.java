package br.com.argo.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.MGEModelException;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class ModelClientFreshSolutions {
	public List<BigDecimal> buscarCodparceFreshSolutions(BigDecimal codpar) throws MGEModelException {
	    JdbcWrapper jdbc = null;
	    NativeSql sql = null;
	    ResultSet rset = null;
	    SessionHandle hnd = null;
	    List<BigDecimal> codparcs = new ArrayList<>();
	    try {
	        hnd = JapeSession.open();
	        hnd.setFindersMaxRows(-1);
	        EntityFacade entity = EntityFacadeFactory.getDWFFacade();
	        jdbc = entity.getJdbcWrapper();
	        jdbc.openSession();
	        sql = new NativeSql(jdbc);
	        sql.appendSql("SELECT CODPARC FROM TGFPAR WHERE CODPARCMATRIZ = 13253");
	        rset = sql.executeQuery();
	        while (rset.next()) {
	            BigDecimal codparc = rset.getBigDecimal("CODPARC");
	            codparcs.add(codparc);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        MGEModelException.throwMe(e);
	        System.out.println("Erro ao tentar buscar buscarCodparceFreshSolutions: " + e.getCause() + e.getMessage());
	    } finally {
	        JapeSession.close(hnd);
	        JdbcWrapper.closeSession(jdbc);
	    }
	    return codparcs;
	}

}
