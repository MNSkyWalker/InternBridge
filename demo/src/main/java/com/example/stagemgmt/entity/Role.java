package com.example.stagemgmt.entity;

public enum Role {
    RESPONSABLE,
    /** Encadreur : superviseur bancaire qui suit le stagiaire au quotidien. Peut se
     *  connecter et a les mêmes fonctions que le responsable, sauf qu'il ne peut pas
     *  créer de nouvelles fiches stagiaire (réservé au responsable). */
    ENCADREUR
}
