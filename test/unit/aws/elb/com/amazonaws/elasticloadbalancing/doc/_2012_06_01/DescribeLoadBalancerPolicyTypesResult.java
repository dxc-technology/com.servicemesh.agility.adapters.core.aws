//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-833 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.06.25 at 11:31:27 AM CDT 
//


package com.amazonaws.elasticloadbalancing.doc._2012_06_01;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *                	<p>
 *                	The output for the <a>DescribeLoadBalancerPolicyTypes</a> action.
 *                	</p>
 *                
 * 
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="PolicyTypeDescriptions" type="{http://elasticloadbalancing.amazonaws.com/doc/2012-06-01/}PolicyTypeDescriptions" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "policyTypeDescriptions"
})
@XmlRootElement(name = "DescribeLoadBalancerPolicyTypesResult")
public class DescribeLoadBalancerPolicyTypesResult {

    @XmlElement(name = "PolicyTypeDescriptions")
    protected PolicyTypeDescriptions policyTypeDescriptions;

    /**
     * Gets the value of the policyTypeDescriptions property.
     * 
     * @return
     *     possible object is
     *     {@link PolicyTypeDescriptions }
     *     
     */
    public PolicyTypeDescriptions getPolicyTypeDescriptions() {
        return policyTypeDescriptions;
    }

    /**
     * Sets the value of the policyTypeDescriptions property.
     * 
     * @param value
     *     allowed object is
     *     {@link PolicyTypeDescriptions }
     *     
     */
    public void setPolicyTypeDescriptions(PolicyTypeDescriptions value) {
        this.policyTypeDescriptions = value;
    }

}