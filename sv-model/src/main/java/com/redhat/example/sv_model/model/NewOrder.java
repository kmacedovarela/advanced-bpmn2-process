package com.redhat.example.sv_model.model;

/**
 * This class was automatically generated by the data modeler tool.
 */

public class NewOrder implements java.io.Serializable
{

   static final long serialVersionUID = 1L;

   private java.util.Date hoaMeetingDate;

   public NewOrder()
   {
   }

   public java.util.Date getHoaMeetingDate()
   {
      return this.hoaMeetingDate;
   }

   public void setHoaMeetingDate(java.util.Date hoaMeetingDate)
   {
      this.hoaMeetingDate = hoaMeetingDate;
   }

   public NewOrder(java.util.Date hoaMeetingDate)
   {
      this.hoaMeetingDate = hoaMeetingDate;
   }

}