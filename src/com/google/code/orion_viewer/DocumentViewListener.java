package com.google.code.orion_viewer;

/**
 * User: mike
 * Date: 13.11.11
 * Time: 17:31
 */
public interface DocumentViewListener {

    public void documentOpened();

    public void documentClosed();

    public void pageChanged(int newPage, int pageCount);

    public void viewParametersChanged();
}
