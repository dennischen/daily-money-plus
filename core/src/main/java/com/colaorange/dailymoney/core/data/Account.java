/**
 * 
 */
package com.colaorange.dailymoney.core.data;

import java.io.Serializable;

/**
 * @author dennis
 *
 */
public class Account implements Serializable{

    private static final long serialVersionUID = 1L;

    private String id;
    
    private String name;
    
    private String type;
    
    private double initialValue;
    
    //is cash account;
    private boolean cashAccount;

    private int priority;
    
    Account(){}
    
    public Account(String type, String name, Double initialValue){
        this.type = type;
        this.name = name==null?"":name.trim();
        this.initialValue = initialValue;
        id = this.name;
    }
    

    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name==null?"":name.trim();
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public boolean isCashAccount() {
        return cashAccount;
    }

    public void setCashAccount(boolean cashAccount) {
        this.cashAccount = cashAccount;
    }

    public double getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(double initialValue) {
        this.initialValue = initialValue;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Account other = (Account) obj;
        if (id == null) {
            return other.id == null;
        } else return id.equals(other.id);
    }


    public Account copy() {
        Account acc = new Account(type, name, initialValue);
        acc.setCashAccount(cashAccount);
        acc.setPriority(priority);
        return acc;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
