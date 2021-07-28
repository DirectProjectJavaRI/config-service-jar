/* 
Copyright (c) 2010, NHIN Direct Project
All rights reserved.

Authors:
   Greg Meyer      gm2552@cerner.com
 
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.  Neither the name of the The NHIN Direct Project (nhindirect.org). 
nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.nhindirect.config.resources.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.nhindirect.config.model.Address;
import org.nhindirect.config.model.Anchor;
import org.nhindirect.config.model.BundleRefreshError;
import org.nhindirect.config.model.CertPolicy;
import org.nhindirect.config.model.CertPolicyGroup;
import org.nhindirect.config.model.CertPolicyGroupDomainReltn;
import org.nhindirect.config.model.CertPolicyGroupUse;
import org.nhindirect.config.model.CertPolicyUse;
import org.nhindirect.config.model.Certificate;
import org.nhindirect.config.model.DNSRecord;
import org.nhindirect.config.model.Domain;
import org.nhindirect.config.model.EntityStatus;
import org.nhindirect.config.model.Setting;
import org.nhindirect.config.model.TrustBundle;
import org.nhindirect.config.model.TrustBundleAnchor;
import org.nhindirect.config.model.exceptions.CertificateConversionException;
import org.nhindirect.config.model.utils.CertUtils;
import org.nhindirect.config.model.utils.CertUtils.CertContainer;
import org.nhindirect.config.store.CertPolicyGroupReltn;
import org.nhindirect.config.store.CertificateException;
import org.nhindirect.policy.PolicyLexicon;

import com.google.common.collect.Maps;

/**
 * Conversion methods from model to entity representation and vice versa.
 * @author Greg Meyer
 * @since 2.0
 */
public class EntityModelConversion 
{
	
	public static Map.Entry<org.nhindirect.config.store.Domain, Collection<org.nhindirect.config.store.Address>> toEntityDomain(Domain domain)
	{
		final org.nhindirect.config.store.Domain retVal = new org.nhindirect.config.store.Domain();
		
		final Collection<org.nhindirect.config.store.Address> addresses = new ArrayList<org.nhindirect.config.store.Address>();
		if (domain.getAddresses() != null)
		{
			boolean postmasterInAddressList = false;
			
			for (Address address : domain.getAddresses())
			{
				addresses.add(toEntityAddress(address, retVal));
				if (domain.getPostmasterAddress() != null &&  address.getEmailAddress().equals(domain.getPostmasterAddress().getEmailAddress()))
				{
					if (address.getId() >= 0)
						retVal.setPostmasterAddressId(address.getId());
					
					postmasterInAddressList = true;
				}
			}
			
			if (!postmasterInAddressList && domain.getPostmasterAddress() != null)
				addresses.add(toEntityAddress(domain.getPostmasterAddress(), retVal));
				
		}
		
		retVal.setCreateTime(localDateTimeFromCalendar(domain.getCreateTime()));
		retVal.setDomainName(domain.getDomainName());
		if (domain.getId() > 0)
			retVal.setId(domain.getId());

		
		if (domain.getStatus() != null)
			retVal.setStatus(org.nhindirect.config.store.EntityStatus.valueOf(domain.getStatus().toString()).ordinal());
		retVal.setUpdateTime(localDateTimeFromCalendar(domain.getUpdateTime()));
		
		return Maps.immutableEntry(retVal, addresses);
	}
	
	public static Domain toModelDomain(org.nhindirect.config.store.Domain domain, List<org.nhindirect.config.store.Address> addrs)
	{
		final Domain retVal = new Domain();
		
		final Collection<Address> addresses = new ArrayList<Address>();
		if (addrs != null)
		{
			for (org.nhindirect.config.store.Address address : addrs)
			{
				addresses.add(toModelAddress(address, domain.getDomainName()));
			}
		}
		retVal.setAddresses(addresses);
		retVal.setCreateTime(calendarFromLocalDateTime(domain.getCreateTime()));
		retVal.setDomainName(domain.getDomainName());
		retVal.setId(domain.getId());

		// get the postmaster address
        if ((addrs.size() > 0) && (domain.getPostmasterAddressId() != null) && (domain.getPostmasterAddressId() > 0)) 
        {
            for (org.nhindirect.config.store.Address address : addrs) 
            {
                if (address.getId().equals(domain.getPostmasterAddressId())) 
                {
        			retVal.setPostmasterAddress(toModelAddress(address, domain.getDomainName()));
        			break;
                }
            }
        }			

		if (domain.getStatus() >= 0)
			retVal.setStatus(EntityStatus.values()[domain.getStatus()]);
		retVal.setUpdateTime(calendarFromLocalDateTime(domain.getUpdateTime()));
		
		return retVal;
	}
	
	public static Address toModelAddress(org.nhindirect.config.store.Address address, String domainName)
	{
    	if (address == null)
    		return null;
    	
    	final Address retVal = new Address();
    	retVal.setCreateTime(calendarFromLocalDateTime(address.getCreateTime()));
    	retVal.setDisplayName(address.getDisplayName());
    	retVal.setEmailAddress(address.getEmailAddress());
    	retVal.setEndpoint(address.getEndpoint());
    	retVal.setId(address.getId());
    	if (address.getStatus() >= 0)
    		retVal.setStatus(EntityStatus.values()[address.getStatus()]);
    	retVal.setType(address.getType());
    	retVal.setUpdateTime(calendarFromLocalDateTime(address.getUpdateTime()));
    	
    	if (!domainName.isEmpty())
    		retVal.setDomainName(domainName);
    	
    	return retVal;
	}
	
    public static org.nhindirect.config.store.Address toEntityAddress(Address address, org.nhindirect.config.store.Domain domain)
    {
    	if (address == null)
    		return null;
    	
    	final org.nhindirect.config.store.Address retVal = new org.nhindirect.config.store.Address();
    	retVal.setCreateTime(localDateTimeFromCalendar(address.getCreateTime()));
    	retVal.setDisplayName(address.getDisplayName());
    	retVal.setEmailAddress(address.getEmailAddress());
    	retVal.setEndpoint(address.getEndpoint());
    	
    	if (address.getId() >= 0)
    		retVal.setId(address.getId());
    	if (address.getStatus() != null)
    		retVal.setStatus(org.nhindirect.config.store.EntityStatus.valueOf(address.getStatus().toString()).ordinal());
    	retVal.setType(address.getType());
    	retVal.setUpdateTime(localDateTimeFromCalendar(address.getUpdateTime()));
    	retVal.setDomainId(domain.getId());
    	
    	return retVal;
    }
    
    public static Anchor toModelAnchor(org.nhindirect.config.store.Anchor anchor)
    {
    	if (anchor == null)
    		return null;
    	
    	final Anchor retVal = new Anchor();
    	
    	retVal.setCertificateData(anchor.getCertificateData());
    	retVal.setCertificateId(anchor.getCertificateId());
    	retVal.setCreateTime(calendarFromLocalDateTime(anchor.getCreateTime()));
    	retVal.setId(anchor.getId());
    	retVal.setIncoming(anchor.isIncoming());
    	retVal.setOutgoing(anchor.isOutgoing());
    	retVal.setOwner(anchor.getOwner());
    	retVal.setStatus(EntityStatus.values()[anchor.getStatus()]);
    	retVal.setThumbprint(anchor.getThumbprint());
    	retVal.setValidEndDate(calendarFromLocalDateTime(anchor.getValidEndDate()));
    	retVal.setValidStartDate(calendarFromLocalDateTime(anchor.getValidStartDate()));
    	
    	return retVal;
    }
    
    public static org.nhindirect.config.store.Anchor toEntityAnchor(Anchor anchor) throws CertificateException
    {
    	if (anchor == null)
    		return null;
    	
    	final org.nhindirect.config.store.Anchor retVal = new org.nhindirect.config.store.Anchor();
    	
    	retVal.setData(anchor.getCertificateData());
    	retVal.setCertificateId(anchor.getCertificateId());
    	retVal.setCreateTime(localDateTimeFromCalendar(anchor.getCreateTime()));
    	
    	if (anchor.getId() >= 0)
    		retVal.setId(anchor.getId());
    	retVal.setIncoming(anchor.isIncoming());
    	retVal.setOutgoing(anchor.isOutgoing());
    	retVal.setOwner(anchor.getOwner());
    	retVal.setStatus(org.nhindirect.config.store.EntityStatus.valueOf(anchor.getStatus().toString()).ordinal());
    	retVal.setValidEndDate(localDateTimeFromCalendar(anchor.getValidEndDate()));
    	retVal.setValidStartDate(localDateTimeFromCalendar(anchor.getValidStartDate()));
    	
    	return retVal;
    }    
    
    public static Certificate toModelCertificate(org.nhindirect.config.store.Certificate cert)
    {
    	if (cert == null)
    		return null;
    	
    	final Certificate retVal = new Certificate();
    	
    	retVal.setOwner(cert.getOwner());
    	retVal.setCreateTime(calendarFromLocalDateTime(cert.getCreateTime()));
    	retVal.setData(cert.getData());
    	retVal.setId(cert.getId());
    	retVal.setPrivateKey(cert.isPrivateKey());
    	if (cert.getStatus() >= 0)
    		retVal.setStatus(EntityStatus.values()[cert.getStatus()]);
    	retVal.setThumbprint(cert.getThumbprint());
    	retVal.setValidEndDate(calendarFromLocalDateTime(cert.getValidEndDate()));
    	retVal.setValidStartDate(calendarFromLocalDateTime(cert.getValidStartDate()));

    	
    	return retVal;
    }   
    
    public static org.nhindirect.config.store.Certificate toEntityCertificate(Certificate cert) throws CertificateException
    {
    	if (cert == null)
    		return null;
    	
    	final org.nhindirect.config.store.Certificate retVal = new org.nhindirect.config.store.Certificate();
    	
    	retVal.setOwner(cert.getOwner());
    	retVal.setCreateTime(localDateTimeFromCalendar(cert.getCreateTime()));
    	retVal.setData(cert.getData());
    	
    	if (cert.getId() >= 0)
    		retVal.setId(cert.getId());
    	
    	if (cert.getStatus() != null)
    		retVal.setStatus(org.nhindirect.config.store.EntityStatus.valueOf(cert.getStatus().toString()).ordinal());
    	
    	final CertContainer cont = CertUtils.toCertContainer(retVal.getData());
    	
    	final Calendar endDate = Calendar.getInstance(Locale.getDefault());
    	endDate.setTime(cont.getCert().getNotAfter());
    	retVal.setValidEndDate(localDateTimeFromCalendar(endDate));
    	
    	final Calendar startDate = Calendar.getInstance(Locale.getDefault());
    	startDate.setTime(cont.getCert().getNotBefore());	
    	retVal.setValidStartDate(localDateTimeFromCalendar(startDate));

    	
    	return retVal;
    }    
    
    public static DNSRecord toModelDNSRecord(org.nhindirect.config.store.DNSRecord record)
    {
    	if (record == null)
    		return null;
    	
    	final DNSRecord retVal = new DNSRecord();
    	
    	retVal.setCreateTime(calendarFromLocalDateTime(record.getCreateTime()));
    	retVal.setData(record.getData());
    	retVal.setDclass(record.getDclass());
    	retVal.setId(record.getId());
    	retVal.setName(record.getName());
    	retVal.setTtl(record.getTtl());
    	retVal.setType(record.getType());
    	
    	return retVal;
    }   
    
    public static org.nhindirect.config.store.DNSRecord toEntityDNSRecord(DNSRecord record)
    {
    	if (record == null)
    		return null;
    	
    	final org.nhindirect.config.store.DNSRecord retVal = new org.nhindirect.config.store.DNSRecord();
    	
    	retVal.setCreateTime(localDateTimeFromCalendar(record.getCreateTime()));
    	retVal.setData(record.getData());
    	retVal.setDclass(record.getDclass());
    	
    	if (record.getId() >=0)
    		retVal.setId(record.getId());
    	
    	retVal.setName(record.getName());
    	retVal.setTtl(record.getTtl());
    	retVal.setType(record.getType());
    	
    	return retVal;
    }    
    
    public static Setting toModelSetting(org.nhindirect.config.store.Setting setting)
    {
    	if (setting == null)
    		return null;
    	
    	final Setting retVal = new Setting();
    	
    	retVal.setId(setting.getId());
    	retVal.setName(setting.getName());
    	if (setting.getStatus() >= 0)
    		retVal.setStatus(EntityStatus.values()[setting.getStatus()]);
    	retVal.setUpdateTime(calendarFromLocalDateTime(setting.getUpdateTime()));
    	retVal.setCreateTime(calendarFromLocalDateTime(setting.getCreateTime()));
    	retVal.setValue(setting.getValue());
    	
    	return retVal;
    }    
    
    public static TrustBundle toModelTrustBundle(org.nhindirect.config.store.TrustBundle bundle, List<org.nhindirect.config.store.TrustBundleAnchor> anchors)
    {
    	if (bundle == null)
    		return null;
    	
    	final TrustBundle retVal = new TrustBundle();
    	
    	final Collection<TrustBundleAnchor> trustAnchors = new ArrayList<TrustBundleAnchor>();
    	
    	if (anchors != null)
    	{
    		for (org.nhindirect.config.store.TrustBundleAnchor anchor : anchors)
    		{
    			final TrustBundleAnchor retAnchor = new TrustBundleAnchor();
    			retAnchor.setAnchorData(anchor.getAnchorData());
    			retAnchor.setThumbprint(anchor.getThumbprint());
    			retAnchor.setId(anchor.getId());
 
    	    	retAnchor.setValidEndDate(calendarFromLocalDateTime(anchor.getValidEndDate()));
    	    	retAnchor.setValidStartDate(calendarFromLocalDateTime(anchor.getValidStartDate()));
    	    	
    	    	trustAnchors.add(retAnchor);
    		}
    	}
    	
    	retVal.setBundleName(bundle.getBundleName());
    	retVal.setBundleURL(bundle.getBundleURL());
    	retVal.setCheckSum(bundle.getCheckSum());
    	retVal.setCreateTime(calendarFromLocalDateTime(bundle.getCreateTime()));
    	retVal.setId(bundle.getId());
    	retVal.setLastRefreshAttempt(calendarFromLocalDateTime(bundle.getLastRefreshAttempt()));
    	if (bundle.getLastRefreshError() >= 0)
    		retVal.setLastRefreshError(BundleRefreshError.values()[bundle.getLastRefreshError()]);
    	
    	retVal.setLastSuccessfulRefresh(calendarFromLocalDateTime(bundle.getLastSuccessfulRefresh()));
    	retVal.setRefreshInterval(bundle.getRefreshInterval());
    	retVal.setSigningCertificateData(bundle.getSigningCertificateData());
    	retVal.setTrustBundleAnchors(trustAnchors);
    	return retVal;
    }  
    
    public static Map.Entry<org.nhindirect.config.store.TrustBundle, Collection<org.nhindirect.config.store.TrustBundleAnchor>> toEntityTrustBundle(TrustBundle bundle)
    {
    	if (bundle == null)
    		return null;
    	
    	final org.nhindirect.config.store.TrustBundle retVal = new org.nhindirect.config.store.TrustBundle();
    	
    	final Collection<org.nhindirect.config.store.TrustBundleAnchor> trustAnchors = new ArrayList<org.nhindirect.config.store.TrustBundleAnchor>();
    	
    	if (bundle.getTrustBundleAnchors() != null)
    	{
    		for (TrustBundleAnchor anchor : bundle.getTrustBundleAnchors())
    		{
    			final org.nhindirect.config.store.TrustBundleAnchor retAnchor = new org.nhindirect.config.store.TrustBundleAnchor();
    			try
    			{
    				retAnchor.setData(anchor.getAnchorData());
    			}
    			catch (CertificateException e) 
    			{
    				throw new CertificateConversionException(e);
				}
    			// the entity object sets all other attributes based on the cert data,
    			// no need to explicitly set it here
    	    	retAnchor.setTrustBundleId(retVal.getId());
    	    	
    	    	trustAnchors.add(retAnchor);
    		}
    	}
    	
    	retVal.setBundleName(bundle.getBundleName());
    	retVal.setBundleURL(bundle.getBundleURL());
    	
    	if (bundle.getCheckSum() == null)
    		retVal.setCheckSum("");
    	else
    		retVal.setCheckSum(bundle.getCheckSum());
    	
    	retVal.setCreateTime((bundle.getCreateTime() != null) ? 
    			localDateTimeFromCalendar(bundle.getCreateTime()) : LocalDateTime.now());
    	
    	if (bundle.getId() >= 0)
    		retVal.setId(bundle.getId());
    	
    	retVal.setLastRefreshAttempt(localDateTimeFromCalendar(bundle.getLastRefreshAttempt()));
    	
    	if (bundle.getLastRefreshError() != null)
    		retVal.setLastRefreshError(org.nhindirect.config.store.BundleRefreshError.valueOf(bundle.getLastRefreshError().toString()).ordinal());
    	
    	retVal.setLastSuccessfulRefresh(localDateTimeFromCalendar(bundle.getLastSuccessfulRefresh()));
    	retVal.setRefreshInterval(bundle.getRefreshInterval());
    	if (bundle.getSigningCertificateData() != null)
			retVal.setSigningCertificateData(bundle.getSigningCertificateData());
 
    	return Maps.immutableEntry(retVal, trustAnchors);
    }   
    
    public static CertPolicy toModelCertPolicy(org.nhindirect.config.store.CertPolicy policy)
    {
    	if (policy == null)
    		return null;
    	
    	final CertPolicy retVal = new CertPolicy();
    	
    	retVal.setPolicyName(policy.getPolicyName());
    	retVal.setCreateTime(calendarFromLocalDateTime(policy.getCreateTime()));
    	if (policy.getLexicon() >= 0)
    		retVal.setLexicon(PolicyLexicon.values()[policy.getLexicon()]);
    	retVal.setPolicyData(policy.getPolicyData());
    	
    	return retVal;
    } 
    
    public static org.nhindirect.config.store.CertPolicy toEntityCertPolicy(CertPolicy policy)
    {
    	if (policy == null)
    		return null;
    	
    	final org.nhindirect.config.store.CertPolicy retVal = new org.nhindirect.config.store.CertPolicy();
    	
    	retVal.setPolicyName(policy.getPolicyName());
    	retVal.setCreateTime((policy.getCreateTime() != null) ? 
    			localDateTimeFromCalendar(policy.getCreateTime()) : LocalDateTime.now());
    	if (policy.getLexicon() != null)
    		retVal.setLexicon(PolicyLexicon.valueOf(policy.getLexicon().toString()).ordinal());
    	retVal.setPolicyData(policy.getPolicyData());
    	
    	return retVal;
    }  
    
    public static CertPolicyGroup toModelCertPolicyGroup(org.nhindirect.config.store.CertPolicyGroup group, Map<CertPolicyGroupReltn, org.nhindirect.config.store.CertPolicy> polUseMap)
    {
    	if (group == null)
    		return null;
    	
    	final CertPolicyGroup retVal = new CertPolicyGroup();
    	
    	final Collection<CertPolicyGroupUse> uses = new ArrayList<CertPolicyGroupUse>();
    	
    	if (!polUseMap.isEmpty())
    	{
    		for (Map.Entry<org.nhindirect.config.store.CertPolicyGroupReltn, org.nhindirect.config.store.CertPolicy> reltnEntry : polUseMap.entrySet())
    		{
    			final CertPolicyGroupUse use = new CertPolicyGroupUse();
    			
    			final CertPolicyGroupReltn reltn = reltnEntry.getKey();
    			
    			use.setPolicy(toModelCertPolicy(reltnEntry.getValue()));
    			if (reltn.getPolicyUse() >= 0)
    				use.setPolicyUse(CertPolicyUse.values()[reltn.getPolicyUse()]);
    			use.setIncoming(reltn.isIncoming());
    			use.setOutgoing(reltn.isOutgoing());

    			uses.add(use);
    		}
    	}
    	
    	retVal.setPolicyGroupName(group.getPolicyGroupName());
    	retVal.setCreateTime(calendarFromLocalDateTime(group.getCreateTime()));
    	retVal.setPolicies(uses);
    	   	
    	return retVal;
    }   
    
    public static org.nhindirect.config.store.CertPolicyGroup toEntityCertPolicyGroup(CertPolicyGroup group)
    {
    	if (group == null)
    		return null;
    	
    	final org.nhindirect.config.store.CertPolicyGroup retVal = new org.nhindirect.config.store.CertPolicyGroup();
    	
    	retVal.setPolicyGroupName(group.getPolicyGroupName());
    	retVal.setCreateTime((group.getCreateTime() != null) ? localDateTimeFromCalendar(group.getCreateTime()) : LocalDateTime.now());
    	
    	   	
    	return retVal;
    }       
    
    public static CertPolicyGroupDomainReltn toModelCertPolicyGroupDomainReltn(Long id, org.nhindirect.config.store.Domain domain, 
    		org.nhindirect.config.store.CertPolicyGroup group, Map<CertPolicyGroupReltn, org.nhindirect.config.store.CertPolicy> polUseMap)
    {    	
    	final CertPolicyGroupDomainReltn retVal = new CertPolicyGroupDomainReltn();
    	
    	retVal.setId(id);
    	retVal.setPolicyGroup(toModelCertPolicyGroup(group, polUseMap));
    	retVal.setDomain(toModelDomain(domain, Collections.emptyList()));

    	return retVal;
    }  
    
    public static Calendar calendarFromLocalDateTime(LocalDateTime time)
    {
    	if (time == null)
    		return null;
    	
    	final Date date = Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    	
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        
        return calendar;
    }
    
    public static LocalDateTime localDateTimeFromCalendar(Calendar time)
    {
        if (time == null) 
            return null;
        
        TimeZone tz = time.getTimeZone();
        ZoneId zid = tz == null ? ZoneId.systemDefault() : tz.toZoneId();
        return LocalDateTime.ofInstant(time.toInstant(), zid);
    }
}
