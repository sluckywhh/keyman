package com.tavultesoft.kmea.cloud.impl;

import java.util.List;
import java.util.Map;

/**
 * key board and lexical model download result.
 */
public class CloudKeyboardDownloadReturns {
  public final Integer kbdResult;
  public final List<Map<String, String>> installedLexicalModels;

  public CloudKeyboardDownloadReturns(Integer i) {
    this(i, null);
  }

  public CloudKeyboardDownloadReturns(Integer i, List<Map<String, String>> installedLexicalModels) {
    this.kbdResult = i;
    this.installedLexicalModels = installedLexicalModels;
  }
}
