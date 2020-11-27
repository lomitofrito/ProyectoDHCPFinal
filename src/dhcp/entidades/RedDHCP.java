package entidades;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

import auxiliares.Auxiliares;

public class RedDHCP
{

    private List<IpArriendo> listaIPsAsignables;
    private byte[] servidorDNS;
    private byte[] gateway;
    private byte[] mascara;
    private int tiempoArrendamiento;
    private byte[] ipRangoInicial;
    private byte[] ipRangoFinal;
    private long ipRangoInicialLong;
    private long ipRangoFinalLong;
    private long ipTemp;
    private byte[] ipActual;
    private boolean rangoCompletado;// bool para rectificar si aún hay disponible ip en red

    public RedDHCP(byte[] servidorDNS, byte[] gateway, byte[] mascara, byte[] ipRangoInicial, byte[] ipRangoFinal,
            int tiempoArrendamiento)
    {
        this.listaIPsAsignables = new ArrayList<>();
        this.servidorDNS = servidorDNS;
        this.gateway = gateway;
        this.mascara = mascara;
        this.ipRangoInicial = ipRangoInicial;
        this.ipRangoFinal = ipRangoFinal;
        this.ipActual = ipRangoInicial.clone();
        this.rangoCompletado = false;
        this.tiempoArrendamiento = tiempoArrendamiento;
    }

    /**
     * @param macCliente
     * @return byte[]
     */
    public byte[] ipOfertado(byte[] macCliente)
    {
        IpArriendo ipArrendamientoActual;

        for (int i = 0; i < listaIPsAsignables.size(); i++)
        {
            ipArrendamientoActual = listaIPsAsignables.get(i);

            if (Auxiliares.compararMacs(macCliente, ipArrendamientoActual.getMac()))
            {
                return ipArrendamientoActual.getIp();
            }
        }

        for (int i = 0; i < listaIPsAsignables.size(); i++)
        {
            ipArrendamientoActual = listaIPsAsignables.get(i);

            if (!ipArrendamientoActual.esArrendado())
            {
                ipArrendamientoActual.setMac(macCliente);
                return ipArrendamientoActual.getIp();
            }
        }

        if (rangoCompletado)
        {
            return null;
        }
        else if (Auxiliares.compararIps(ipActual, ipRangoFinal))
        {
            rangoCompletado = true;
        }

        ipArrendamientoActual = new IpArriendo(ipActual.clone(), macCliente);

        boolean siguienteIp = false;
        for (int i = ipActual.length; i >= 0; i--)
        {
            if (i != 3)
            {
                ipActual[i - 1] = 0;
            }

            if (ipActual[i] < (byte) 255 && !siguienteIp)
            {
                ipActual[i] = (byte) (Auxiliares.unsignedToBytes(ipActual[i]) + 1);
                siguienteIp = true;
                break;
            }
        }
        if (siguienteIp == false)
        {
            rangoCompletado = true;
            return null;
        }

        listaIPsAsignables.add(ipArrendamientoActual);
        return ipArrendamientoActual.getIp();
    }

    /**
     * @param ipCliente
     * @return IpArriendo
     */
    public IpArriendo verificarIp(byte[] ipCliente)
    {
        IpArriendo ipArrendamientoActual;

        for (int i = 0; i < listaIPsAsignables.size(); i++)
        {
            ipArrendamientoActual = listaIPsAsignables.get(i);

            if (Auxiliares.compararIps(ipArrendamientoActual.getIp(), ipCliente))
            {
                return ipArrendamientoActual;
            }
        }
        return null;
    }

    /**
     * @param ip
     * @return IpArriendo
     */
    // Se agrega a la lista de Ips si la ip esta disponible
    // Se retorna la ip.
    public IpArriendo agregarIp(byte[] ip)
    {
        IpArriendo temp = verificarIp(ip);
        ipRangoInicialLong = Auxiliares.ipALong(this.ipRangoInicial);
        ipRangoFinalLong = Auxiliares.ipALong(this.ipRangoFinal);
        ipTemp = Auxiliares.ipALong(ip);
        if (temp == null)
        {
            if (ipTemp < ipRangoFinalLong && ipTemp > ipRangoInicialLong)
            {
                temp = new IpArriendo(ip);
                listaIPsAsignables.add(temp);
            }
            else
            {
                return null;
            }

        }
        return temp;
    }

    /**
     * @param ip
     * @param tiempoArrendamiento
     * @param mac
     */
    public void asignarIp(IpArriendo ip, int tiempoArrendamiento, byte[] mac)
    {
        ip.setMac(mac);
        ip.setArrendado(true);
        ip.setTiempoInicio(new GregorianCalendar());
        ip.setTiempoFinal(new GregorianCalendar());
        ip.getTiempoFinal().add(GregorianCalendar.SECOND, tiempoArrendamiento);
    }

    /**
     * @param ipCliente
     */
    public void liberarIp(byte[] ipCliente)
    {
        IpArriendo ipArrendamientoActual;

        for (int i = 0; i < listaIPsAsignables.size(); i++)
        {
            ipArrendamientoActual = listaIPsAsignables.get(i);

            if (Auxiliares.compararIps(ipArrendamientoActual.getIp(), ipCliente))
            {
                ipArrendamientoActual.setArrendado(false);
                break;
            }
        }
    }

    /**
     * @param ipCliente
     * @param tiempoArrendamiento
     * @return boolean
     */
    public boolean renovarTiempoArrendamiento(byte[] ipCliente, int tiempoArrendamiento)
    {
        IpArriendo temp = verificarIp(ipCliente);
        if (temp == null)
        {
            return false;
        }
        temp.setTiempoFinal(new GregorianCalendar());
        temp.getTiempoFinal().add(GregorianCalendar.SECOND, tiempoArrendamiento);
        return true;
    }

    public void verificarCaducidadLease()
    {
        IpArriendo ipArrendamientoActual;
        GregorianCalendar horaActual;

        for (int i = 0; i < listaIPsAsignables.size(); i++)
        {
            ipArrendamientoActual = listaIPsAsignables.get(i);
            horaActual = new GregorianCalendar();

            if (horaActual.after(ipArrendamientoActual.getTiempoFinal()))
            {
                ipArrendamientoActual.setArrendado(false);
            }
            break;
        }
    }

    /**
     * @return List<IpArriendo>
     */
    public List<IpArriendo> getListaIPsAsignables()
    {
        return this.listaIPsAsignables;
    }

    /**
     * @param listaIPsAsignables
     */
    public void setListaIPsAsignables(List<IpArriendo> listaIPsAsignables)
    {
        this.listaIPsAsignables = listaIPsAsignables;
    }

    /**
     * @return byte[]
     */
    public byte[] getServidorDNS()
    {
        return this.servidorDNS;
    }

    /**
     * @param servidorDNS
     */
    public void setServidorDNS(byte[] servidorDNS)
    {
        this.servidorDNS = servidorDNS;
    }

    /**
     * @return byte[]
     */
    public byte[] getGateway()
    {
        return this.gateway;
    }

    /**
     * @param gateway
     */
    public void setGateway(byte[] gateway)
    {
        this.gateway = gateway;
    }

    /**
     * @return byte[]
     */
    public byte[] getMascara()
    {
        return this.mascara;
    }

    /**
     * @param mascara
     */
    public void setMascara(byte[] mascara)
    {
        this.mascara = mascara;
    }

    /**
     * @return int
     */
    public int getTiempoArrendamiento()
    {
        return this.tiempoArrendamiento;
    }

    /**
     * @param tiempoArrendamiento
     */
    public void setTiempoArrendamiento(int tiempoArrendamiento)
    {
        this.tiempoArrendamiento = tiempoArrendamiento;
    }

    /**
     * @return byte[]
     */
    public byte[] getIpRangoInicial()
    {
        return this.ipRangoInicial;
    }

    /**
     * @param ipRangoInicial
     */
    public void setIpRangoInicial(byte[] ipRangoInicial)
    {
        this.ipRangoInicial = ipRangoInicial;
    }

    /**
     * @return byte[]
     */
    public byte[] getIpRangoFinal()
    {
        return this.ipRangoFinal;
    }

    /**
     * @param ipRangoFinal
     */
    public void setIpRangoFinal(byte[] ipRangoFinal)
    {
        this.ipRangoFinal = ipRangoFinal;
    }

    /**
     * @return byte[]
     */
    public byte[] getIpActual()
    {
        return this.ipActual;
    }

    /**
     * @param ipActual
     */
    public void setIpActual(byte[] ipActual)
    {
        this.ipActual = ipActual;
    }

    /**
     * @return boolean
     */
    public boolean isRangoCompletado()
    {
        return this.rangoCompletado;
    }

    /**
     * @return boolean
     */
    public boolean getRangoCompletado()
    {
        return this.rangoCompletado;
    }

    /**
     * @param rangoCompletado
     */
    public void setRangoCompletado(boolean rangoCompletado)
    {
        this.rangoCompletado = rangoCompletado;
    }

    /**
     * @param o
     * @return boolean
     */
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }
        if (!(o instanceof RedDHCP))
        {
            return false;
        }
        RedDHCP redDHCP = (RedDHCP) o;
        return Objects.equals(listaIPsAsignables, redDHCP.listaIPsAsignables)
                && Objects.equals(servidorDNS, redDHCP.servidorDNS) && Objects.equals(gateway, redDHCP.gateway)
                && Objects.equals(mascara, redDHCP.mascara) && tiempoArrendamiento == redDHCP.tiempoArrendamiento
                && Objects.equals(ipRangoInicial, redDHCP.ipRangoInicial)
                && Objects.equals(ipRangoFinal, redDHCP.ipRangoFinal) && Objects.equals(ipActual, redDHCP.ipActual)
                && rangoCompletado == redDHCP.rangoCompletado;
    }

    /**
     * @return int
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(listaIPsAsignables, servidorDNS, gateway, mascara, tiempoArrendamiento, ipRangoInicial,
                ipRangoFinal, ipActual, rangoCompletado);
    }

    /**
     * @return String
     */
    @Override
    public String toString()
    {
        return "{" + " listaIPsAsignables='" + getListaIPsAsignables() + "'" + ", servidorDNS='" + getServidorDNS()
                + "'" + ", gateway='" + getGateway() + "'" + ", mascara='" + getMascara() + "'"
                + ", tiempoArrendamiento='" + getTiempoArrendamiento() + "'" + ", ipRangoInicial='"
                + getIpRangoInicial() + "'" + ", ipRangoFinal='" + getIpRangoFinal() + "'" + ", ipActual='"
                + getIpActual() + "'" + ", rangoCompletado='" + isRangoCompletado() + "'" + "}";
    }

}
