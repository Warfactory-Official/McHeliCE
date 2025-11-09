package com.norwood.mcheli.eval.eval.lex;

import java.util.List;

import com.norwood.mcheli.eval.eval.exp.ShareExpValue;
import com.norwood.mcheli.eval.eval.rule.ShareRuleValue;

public class LexFactory {

    public Lex create(String str, List<String>[] opeList, ShareRuleValue share, ShareExpValue exp) {
        return new Lex(str, opeList, share.paren, exp);
    }
}
