package com.ten.ware.dbsplit.core.sql.parser;

import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.alibaba.druid.sql.parser.Lexer;
import com.alibaba.druid.sql.parser.Token;
import com.ten.ware.dbsplit.core.sql.parser.SplitSqlStructure.SqlType;
import com.ten.ware.dbsplit.excep.NotSupportedException;

/**
 * SQL分片解析
 */
public class SplitSqlParserDefImpl implements SplitSqlParser {
    private static final Logger log = LoggerFactory
            .getLogger(SplitSqlParserDefImpl.class);

    private static final int CACHE_SIZE = 1000;

    @SuppressWarnings("unchecked")
    private Map<String, SplitSqlStructure> cache = new LRUMap(CACHE_SIZE);

    public SplitSqlParserDefImpl() {
        log.info("Default SplitSqlParserDefImpl is used.");
    }

    /**
     * SQL分片解析方法：使用阿里巴巴druid库进行
     */
    @Override
    public SplitSqlStructure parseSplitSql(String sql) {
        SplitSqlStructure splitSqlStructure = cache.get(sql);

        // 缓存
        // Don't use if contains then get, race conditon may happens due to LRU
        // map
        if (splitSqlStructure != null) {
            return splitSqlStructure;
        }

        splitSqlStructure = new SplitSqlStructure();

        String dbName = null;
        String tableName = null;
        boolean inProcess = false;

        boolean previous = true;
        boolean sebsequent = false;
        StringBuffer sbPreviousPart = new StringBuffer();
        StringBuffer sbSebsequentPart = new StringBuffer();

        // Need to opertimize for better performance

        // alibaba SQL解析库
        Lexer lexer = new Lexer(sql);
        do {
            lexer.nextToken();
            Token tok = lexer.token();
            if (tok == Token.EOF) {
                break;
            }

            if (tok.name != null) {
                switch (tok.name) {
                    case "SELECT":
                        splitSqlStructure.setSqlType(SqlType.SELECT);
                        break;

                    case "INSERT":
                        splitSqlStructure.setSqlType(SqlType.INSERT);
                        break;

                    case "UPDATE":
                        inProcess = true;
                        splitSqlStructure.setSqlType(SqlType.UPDATE);
                        break;

                    case "DELETE":
                        splitSqlStructure.setSqlType(SqlType.DELETE);
                        break;

                    case "INTO":
                        if (SqlType.INSERT.equals(splitSqlStructure.getSqlType())) {
                            inProcess = true;
                        }
                        break;

                    case "FROM":
                        if (SqlType.SELECT.equals(splitSqlStructure.getSqlType())
                                || SqlType.DELETE.equals(splitSqlStructure
                                .getSqlType())) {
                            inProcess = true;
                        }
                        break;
                    default:
                        break;
                }
            }

            if (sebsequent) {
                sbSebsequentPart.append(
                        tok == Token.IDENTIFIER ? lexer.stringVal() : tok.name)
                        .append(" ");
            }

            if (inProcess) {
                if (dbName == null && tok == Token.IDENTIFIER) {
                    dbName = lexer.stringVal();
                    previous = false;
                } else if (dbName != null && tableName == null
                        && tok == Token.IDENTIFIER) {
                    tableName = lexer.stringVal();

                    inProcess = false;
                    sebsequent = true;
                }
            }

            if (previous) {
                sbPreviousPart.append(
                        tok == Token.IDENTIFIER ? lexer.stringVal() : tok.name)
                        .append(" ");
            }

        } while (true);

        if (StringUtils.isEmpty(dbName) || StringUtils.isEmpty(tableName)) {
            throw new NotSupportedException("The split sql is not supported: "
                    + sql);
        }

        splitSqlStructure.setDbName(dbName);
        splitSqlStructure.setTableName(tableName);

        splitSqlStructure.setPreviousPart(sbPreviousPart.toString());
        splitSqlStructure.setSebsequentPart(sbSebsequentPart.toString());

        // if race condition, it is not severe
        if (!cache.containsKey(splitSqlStructure)) {
            cache.put(sql, splitSqlStructure);
        }

        return splitSqlStructure;
    }
}
