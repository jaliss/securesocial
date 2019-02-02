package securesocial.controllers

import javax.inject.Inject

import play.api.http.HttpErrorHandler

class Assets @Inject() (errorHandler: HttpErrorHandler, metadata: controllers.AssetsMetadata)
  extends controllers.AssetsBuilder(errorHandler, metadata)